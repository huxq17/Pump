package com.huxq17.download.demo;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.huxq17.download.DownloadInfo;
import com.huxq17.download.Pump;
import com.huxq17.download.listener.DownloadObserver;

import java.util.HashMap;
import java.util.List;

public class DownloadListActivity extends AppCompatActivity {
    DownloadObserver downloadObserver = new DownloadObserver() {
        @Override
        public void onProgress(int progress) {
            DownloadInfo downloadInfo = getDownloadInfo();
            DownloadViewHolder viewHolder = (DownloadViewHolder) downloadInfo.getTag();
            if (viewHolder != null) {
                DownloadInfo tag = map.get(viewHolder);
                if (tag != null && tag.getFilePath().equals(downloadInfo.getFilePath())) {
                    viewHolder.bindData(downloadInfo);
                }
            }
        }
    };
    private HashMap<DownloadViewHolder, DownloadInfo> map = new HashMap<>();
    private RecyclerView recyclerView;
    private DownloadAdapter downloadAdapter;
    private List<? extends DownloadInfo> downloadInfoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_list);
        Pump.subscribe(downloadObserver);
        recyclerView = findViewById(R.id.rvDownloadList);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        //获取全部下载任务
        downloadInfoList = Pump.getAllDownloadList();
        recyclerView.setLayoutManager(linearLayoutManager);
        downloadAdapter = new DownloadAdapter(map, downloadInfoList);
        recyclerView.setAdapter(downloadAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Pump.unSubscribe(downloadObserver);
        for (DownloadInfo downloadInfo : downloadInfoList) {
            Pump.stop(downloadInfo);
        }
    }

    public static class DownloadAdapter extends RecyclerView.Adapter<DownloadViewHolder> {
        List<? extends DownloadInfo> downloadInfoList;
        HashMap<DownloadViewHolder, DownloadInfo> map;

        public DownloadAdapter(HashMap<DownloadViewHolder, DownloadInfo> map, List<? extends DownloadInfo> downloadInfoList) {
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
        public void onBindViewHolder(@NonNull DownloadViewHolder viewHolder, int i) {
            DownloadInfo downloadInfo = downloadInfoList.get(i);
            viewHolder.bindData(downloadInfo);

            downloadInfo.setTag(viewHolder);
            map.put(viewHolder, downloadInfo);
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
        TextView tvDownload;
        DownloadInfo downloadInfo;
        private String totalSizeString;
        long totalSize;
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
                            Pump.delete(downloadInfo);
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
//            int progress = downloadInfo.getProgress();
            tvName.setText(downloadInfo.getName());
            String speed = "";
            int progress = downloadInfo.getProgress();
            progressBar.setProgress(progress);
            switch (downloadInfo.getStatus()) {
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
                    tvStatus.setText("Finished");
                    break;
                case FAILED:
                    tvStatus.setText("Retry");
                    break;
            }
            tvSpeed.setText(speed);
            long completedSize = downloadInfo.getCompletedSize();
            if (totalSize == 0) {
                long totalSize = downloadInfo.getContentLength();
                totalSizeString = "/" + Util.getDataSize(totalSize);
            }
            tvDownload.setText(Util.getDataSize(completedSize) + totalSizeString);
        }

        @Override
        public void onClick(View v) {
            switch (downloadInfo.getStatus()) {
                case STOPPED:
                    Pump.download(downloadInfo.getUrl(), downloadInfo.getFilePath());
                    break;
                case PAUSED:
                    Pump.resume(downloadInfo);
                    break;
                case WAIT:
                    //do nothing.
                    break;
                case RUNNING:
                    Pump.pause(downloadInfo);
                    break;
                case FINISHED:
                    Toast.makeText(v.getContext(), "Download Completed.", Toast.LENGTH_SHORT).show();
                    break;
                case FAILED:
                    Pump.resume(downloadInfo);
                    break;
            }
        }

        @Override
        public boolean onLongClick(View v) {
            dialog.show();
            return true;
        }
    }

}
