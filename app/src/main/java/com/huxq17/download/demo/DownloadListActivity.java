package com.huxq17.download.demo;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.huxq17.download.Pump;
import com.huxq17.download.core.DownloadInfo;
import com.huxq17.download.core.DownloadListener;
import com.huxq17.download.demo.installapk.APK;
import com.huxq17.download.utils.LogUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class DownloadListActivity extends AppCompatActivity {
    public static void start(Context context, String tag) {
        Intent intent = new Intent(context, DownloadListActivity.class);
        intent.putExtra("tag", tag);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    private DownloadListener downloadListener = new DownloadListener(this) {
        @Override
        public void onProgress(int progress) {
            DownloadInfo downloadInfo = getDownloadInfo();
            Object extraData = downloadInfo.getExtraData();
            if (!(extraData instanceof DownloadViewHolder)) return;
            DownloadViewHolder viewHolder = (DownloadViewHolder) extraData;
            DownloadInfo tag = map.get(viewHolder);
            if (tag != null && tag.getId().equals(downloadInfo.getId())) {
                viewHolder.bindData(downloadInfo);
            }
        }

        @Override
        public void onFailed() {
            super.onFailed();
            LogUtil.e("onFailed id=" + getDownloadInfo().getId() + ";code=" + getDownloadInfo().getErrorCode());
        }
    };
    private final HashMap<DownloadViewHolder, DownloadInfo> map = new HashMap<>();
    private RecyclerView recyclerView;
    private DownloadAdapter downloadAdapter;
    private List<DownloadInfo> downloadInfoList;
    private String tag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tag = getIntent().getStringExtra("tag");
        setContentView(R.layout.activity_download_list);
        recyclerView = findViewById(R.id.rvDownloadList);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        //Get all download list.
        downloadInfoList = TextUtils.isEmpty(tag) ? Pump.getAllDownloadList() : Pump.getDownloadListByTag(tag);
        for (DownloadInfo downloadInfo : downloadInfoList) {
            LogUtil.e("id="+downloadInfo.getId()+";createTime="+downloadInfo.getCreateTime());
        }
        //Sort download list if need，default sort by createTime DESC
//        Collections.sort(downloadInfoList, new Comparator<DownloadInfo>() {
//            @Override
//            public int compare(DownloadInfo o1, DownloadInfo o2) {
//                return (int) (o1.getCreateTime() - o2.getCreateTime());
//            }
//        });
        recyclerView.setLayoutManager(linearLayoutManager);
        downloadAdapter = new DownloadAdapter(map, downloadInfoList);
        recyclerView.setAdapter(downloadAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (DownloadInfo downloadInfo : downloadInfoList) {
            Pump.stop(downloadInfo.getId());
        }
//        Pump.shutdown();
    }

    public static class DownloadAdapter extends RecyclerView.Adapter<DownloadViewHolder> {
        List<DownloadInfo> downloadInfoList;
        HashMap<DownloadViewHolder, DownloadInfo> map;

        public DownloadAdapter(HashMap<DownloadViewHolder, DownloadInfo> map, List<DownloadInfo> downloadInfoList) {
            this.downloadInfoList = downloadInfoList;
            this.map = map;
        }

        @NonNull
        @Override
        public DownloadViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_download_list, viewGroup, false);
            return new DownloadViewHolder(v, this);
        }

        @Override
        public void onBindViewHolder(@NonNull DownloadViewHolder viewHolder, int position) {
            DownloadInfo downloadInfo = downloadInfoList.get(position);
            downloadInfo.setExtraData(viewHolder);
            map.put(viewHolder, downloadInfo);

            viewHolder.bindData(downloadInfo);
        }

        public void delete(DownloadViewHolder viewHolder) {
            int position = viewHolder.getAdapterPosition();
            downloadInfoList.remove(position);
            notifyItemRemoved(position);
            map.remove(viewHolder);
        }

        @Override
        public int getItemCount() {
            return downloadInfoList.size();
        }
    }

    public static class DownloadViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        ProgressBar progressBar;
        TextView tvName;
        TextView tvStatus;
        TextView tvSpeed;
        TextView tvCreateTime;
        TextView tvDownload;
        DownloadInfo downloadInfo;
        DownloadInfo.Status status;
        AlertDialog dialog;

        public DownloadViewHolder(@NonNull View itemView, final DownloadAdapter adapter) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.pb_progress);
            tvName = itemView.findViewById(R.id.tv_name);
            tvStatus = itemView.findViewById(R.id.bt_status);
            tvSpeed = itemView.findViewById(R.id.tv_speed);
            tvDownload = itemView.findViewById(R.id.tv_download);
            tvCreateTime = itemView.findViewById(R.id.tvCreateTime);
            tvStatus.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            dialog = new AlertDialog.Builder(itemView.getContext())
                    .setTitle("Confirm delete?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            adapter.delete(DownloadViewHolder.this);
                            Pump.deleteById(downloadInfo.getId());
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .create();
        }

        public void bindData(DownloadInfo downloadInfo) {
            this.downloadInfo = downloadInfo;
            this.status = downloadInfo.getStatus();
            tvName.setText(downloadInfo.getName());
            String speed = "";
            int progress = downloadInfo.getProgress();
            progressBar.setProgress(progress);

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss.sss");
            String createTime =format.format(new Date(downloadInfo.getCreateTime()));
            tvCreateTime.setText(createTime);
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
                    tvStatus.setText("Install");
                    break;
                case FAILED:
                    tvStatus.setText("Retry");
                    break;
            }
            tvSpeed.setText(speed);
            long completedSize = downloadInfo.getCompletedSize();
            long totalSize = downloadInfo.getContentLength();
            tvDownload.setText(Utils.getDataSize(completedSize) + "/" + Utils.getDataSize(totalSize));
        }

        @Override
        public void onClick(View v) {
            if (v == tvStatus) {
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
                        Uri contentUri = downloadInfo.getContentUri();
                        if (contentUri != null) {
                            APK.with(itemView.getContext())
                                    .from(contentUri)
                                    .install();
                        } else {
                            APK.with(itemView.getContext())
                                    .from(downloadInfo.getFilePath())
                                    .install();
                        }

//                        Context context = v.getContext();
//                        File videoFile = new File(downloadInfo.getFilePath());
//                        Intent intent = new Intent(Intent.ACTION_VIEW);
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                            Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileProvider-installApk", videoFile);
//                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                            intent.setDataAndType(contentUri, "video/*");
//                        } else {
//                            Uri contentUri = Uri.fromFile(videoFile);
//                            intent.setDataAndType(contentUri, "video/*");
//                        }
//                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                        v.getContext().startActivity(Intent.createChooser(intent, "播放"));
                        break;
                }
            }

        }

        @Override
        public boolean onLongClick(View v) {
            dialog.show();
            return true;
        }
    }

}
