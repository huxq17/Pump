package com.huxq17.download.demo.remote;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.huxq17.download.Pump;
import com.huxq17.download.core.DownloadInfo;
import com.huxq17.download.core.DownloadListener;
import com.huxq17.download.demo.R;
import com.huxq17.download.demo.RxPump;
import com.huxq17.download.demo.Utils;
import com.huxq17.download.utils.LogUtil;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class RemoteDownloadListActivity extends AppCompatActivity {
    public static void start(Context context) {
        Intent intent = new Intent(context, RemoteDownloadListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    DownloadListener downloadObserver = new DownloadListener(this) {
        @Override
        public void onProgress(int progress) {
            DownloadInfo downloadInfo = getDownloadInfo();
            DownloadViewHolder viewHolder = map.get(downloadInfo.getId());
            if (viewHolder != null) {
                Music music = viewHolder.getMusic();
                music.downloadInfo = downloadInfo;
                viewHolder.bindData(music);
                String id = downloadInfo.getId();
                if (!downloadingMusicList.contains(id)) {
                    downloadingMusicList.add(downloadInfo.getId());
                    storeDownloadingList();
                }
                DownloadInfo.Status status = downloadInfo.getStatus();
                if (status == DownloadInfo.Status.PAUSING
                        || status == DownloadInfo.Status.PAUSED) {
                    downloadingMusicList.remove(getDownloadInfo().getId());
                    storeDownloadingList();
                }
            }
        }

        @Override
        public void onSuccess() {
            super.onSuccess();
            downloadingMusicList.remove(getDownloadInfo().getId());
            storeDownloadingList();
        }

        @Override
        public void onFailed() {
            super.onFailed();
            LogUtil.e("onFailed code=" + getDownloadInfo().getErrorCode());
            downloadingMusicList.remove(getDownloadInfo().getId());
            storeDownloadingList();
        }

        private void storeDownloadingList() {
            DownloadingTaskStore.storeDownloadingList(downloadingMusicList);
        }
    };
    private Map<String, DownloadViewHolder> map = new HashMap<>();
    private RecyclerView recyclerView;
    private DownloadAdapter downloadAdapter;
    private Disposable disposable;
    private LinkedHashSet<String> downloadingMusicList = new LinkedHashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_list);
        downloadObserver.enable();
        recyclerView = findViewById(R.id.rvDownloadList);

        Observable<Map<String, DownloadInfo>> downloadList = RxPump.getAllDownloadList()
                .concatMap(new Function<List<DownloadInfo>, ObservableSource<DownloadInfo>>() {
                    @Override
                    public ObservableSource<DownloadInfo> apply(List<DownloadInfo> downloadInfoList) {
                        return Observable.fromIterable(downloadInfoList);
                    }
                })
                .toMap(new Function<DownloadInfo, String>() {
                    @Override
                    public String apply(DownloadInfo downloadInfo) {
                        return downloadInfo.getId();
                    }
                }).toObservable()
                .subscribeOn(Schedulers.io());
        Observable<List<Music>> musicListObservable = Network.getMusicList()
                .subscribeOn(Schedulers.io());
        disposable = Observable.zip(downloadList, musicListObservable,
                new BiFunction<Map<String, DownloadInfo>, List<Music>, List<Music>>() {
                    @Override
                    public List<Music> apply(Map<String, DownloadInfo> downloadInfoMap, List<Music> musicList) {
                        for (Music music : musicList) {
                            String id = music.id;
                            music.downloadInfo = downloadInfoMap.get(id);
                        }
                        downloadingMusicList = DownloadingTaskStore.restoreDownloadingList();
                        return musicList;
                    }
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<Music>>() {
                    @Override
                    public void accept(List<Music> musicList) {
                        downloadAdapter = new DownloadAdapter(map, musicList);
                        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
                        recyclerView.setLayoutManager(linearLayoutManager);
                        recyclerView.setAdapter(downloadAdapter);
                        resumeDownloadTask();
                    }
                });
    }

    private void resumeDownloadTask() {
        for (String id : downloadingMusicList) {
            Pump.newRequest(id).disableBreakPointDownload().submit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.dispose();
        for (String id : map.keySet()) {
            Pump.stop(id);
        }
//        Pump.shutdown();
    }

    public static class DownloadAdapter extends RecyclerView.Adapter<DownloadViewHolder> {
        List<Music> musicList;
        Map<String, DownloadViewHolder> map;

        public DownloadAdapter(Map<String, DownloadViewHolder> map, List<Music> musicList) {
            this.musicList = musicList;
            this.map = map;
        }

        @NonNull
        @Override
        public DownloadViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_download_list, viewGroup, false);
            return new DownloadViewHolder(v, this);
        }

        @SuppressLint("CheckResult")
        @Override
        public void onBindViewHolder(@NonNull final DownloadViewHolder viewHolder, int i) {
            final Music music = musicList.get(i);
            DownloadInfo downloadInfo = music.downloadInfo;
            if (downloadInfo != null) {
                downloadInfo.setExtraData(viewHolder);
            }
            map.put(music.id, viewHolder);
            RxPump.getDownloadInfoById(music.id)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<DownloadInfo>() {
                        @Override
                        public void accept(DownloadInfo downloadInfo) throws Exception {
                            music.downloadInfo = downloadInfo;
                            viewHolder.bindData(music);
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            viewHolder.bindData(music);
                        }
                    });
        }

        public void delete(DownloadViewHolder viewHolder) {
            int position = viewHolder.getAdapterPosition();
            Music music = musicList.remove(position);
            notifyItemRemoved(position);
            map.remove(music.id);
        }

        @Override
        public int getItemCount() {
            return musicList.size();
        }
    }

    public static class DownloadViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        ProgressBar progressBar;
        TextView tvName;
        TextView tvStatus;
        TextView tvSpeed;
        TextView tvDownload;
        Music music;
        DownloadInfo.Status status;
        AlertDialog dialog;

        public DownloadViewHolder(@NonNull View itemView, final DownloadAdapter adapter) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.pb_progress);
            tvName = itemView.findViewById(R.id.tv_name);
            tvStatus = itemView.findViewById(R.id.bt_status);
            tvSpeed = itemView.findViewById(R.id.tv_speed);
            tvDownload = itemView.findViewById(R.id.tv_download);
            tvStatus.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            dialog = new AlertDialog.Builder(itemView.getContext())
                    .setTitle("Confirm delete?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            adapter.delete(DownloadViewHolder.this);
                            Pump.deleteById(music.downloadInfo.getId());
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .create();
        }

        public Music getMusic() {
            return music;
        }

        public void bindData(Music music) {
            this.music = music;

            DownloadInfo downloadInfo = music.downloadInfo;
            if (downloadInfo != null) {
                this.status = downloadInfo.getStatus();
                String speed = "";
                int progress = downloadInfo.getProgress();
                progressBar.setProgress(progress);
                switch (status) {
                    case STOPPED:
                        tvStatus.setText("Start");
                        break;
                    case PAUSING:
                        tvStatus.setText("Pausing");
                        break;
                    case PAUSED:
                        tvStatus.setText("Continue");
                        break;
                    case WAIT:
                        tvStatus.setText("Waiting");
                        break;
                    case RUNNING:
                        tvStatus.setText("Pause");
                        speed = downloadInfo.getSpeed();
                        break;
                    case FINISHED:
                        tvStatus.setText("Play");
                        break;
                    case FAILED:
                        tvStatus.setText("Retry");
                        break;
                }
                tvSpeed.setText(speed);
                long completedSize = downloadInfo.getCompletedSize();
                long totalSize = downloadInfo.getContentLength();
                tvDownload.setText(Utils.getDataSize(completedSize) + "/" + Utils.getDataSize(totalSize));
            } else {
                tvSpeed.setText("");
                tvDownload.setText("");
                tvStatus.setText("Start");
                progressBar.setProgress(0);
            }
            tvName.setText(music.name);
        }

        private void playMusic(String filePath) {
            Context context = itemView.getContext();
            File mp3File = new File(filePath);
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            String type = "audio/*";
            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileProvider-installApk", mp3File);
            } else {
                uri = Uri.fromFile(mp3File);
            }
            intent.setDataAndType(uri, type);
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException ignore) {
                Toast.makeText(context, "Do you have an mp3 player?", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onClick(View v) {
            if (v == tvStatus) {
                DownloadInfo downloadInfo = music.downloadInfo;
                if (downloadInfo != null) {
                    switch (status) {
                        case STOPPED:
                        case PAUSED:
                        case FAILED:
                            Pump.resume(downloadInfo.getId());
                            break;
                        case WAIT:
                            //do nothing.
                            break;
                        case RUNNING:
                            Pump.pause(downloadInfo.getId());
                            break;
                        case FINISHED:
                            playMusic(downloadInfo.getFilePath());
                            break;
                    }
                } else {
                    Pump.newRequest(music.url).disableBreakPointDownload().submit();
                }
            }

        }

        @Override
        public boolean onLongClick(View v) {
            if (music.downloadInfo == null) {
                return false;
            }
            dialog.show();
            return true;
        }
    }

}
