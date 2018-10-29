package com.huxq17.download.demo;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.huxq17.download.DownloadInfo;
import com.huxq17.download.Pump;
import com.huxq17.download.listener.DownloadObserver;

import java.util.HashMap;
import java.util.List;

public class DownloadListActivity extends AppCompatActivity {
    DownloadObserver downloadObserver = new DownloadObserver() {
        @Override
        public void onProgressUpdate(int progress) {
            DownloadInfo downloadInfo = getDownloadInfo();
            DownloadViewHolder viewHolder = (DownloadViewHolder) downloadInfo.getTag();
            Log.e("main", "Main progress=" + progress + ";filePath=" + downloadInfo.getFilePath());
            if (viewHolder != null) {
                DownloadInfo tag = map.get(viewHolder);
                if (tag.getFilePath().equals(downloadInfo.getFilePath())) {
                    viewHolder.bindData(downloadInfo);
                }
            }
        }

        @Override
        public void onError(int errorCode) {

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
        downloadInfoList = Pump.getAllDownloadList();
        recyclerView.setLayoutManager(linearLayoutManager);
        downloadAdapter = new DownloadAdapter(map, downloadInfoList);
        recyclerView.setAdapter(downloadAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Pump.unSubscribe(downloadObserver);
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
            return new DownloadViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull DownloadViewHolder viewHolder, int i) {
            DownloadInfo downloadInfo = downloadInfoList.get(i);
            viewHolder.bindData(downloadInfo);

            downloadInfo.setTag(viewHolder);
            map.put(viewHolder, downloadInfo);
        }

        @Override
        public int getItemCount() {
            return downloadInfoList.size();
        }
    }

    public static class DownloadViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ProgressBar progressBar;
        TextView tvName;
        TextView tvStatus;
        DownloadInfo downloadInfo;

        public DownloadViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.pb_progress);
            tvName = itemView.findViewById(R.id.tv_name);
            tvStatus = itemView.findViewById(R.id.bt_status);
            tvStatus.setOnClickListener(this);
        }

        public void bindData(DownloadInfo downloadInfo) {
            this.downloadInfo = downloadInfo;
            int progress = downloadInfo.getProgress();
            tvName.setText(downloadInfo.getName());
            progressBar.setProgress(progress);
        }

        @Override
        public void onClick(View v) {
            Log.e("tag", "onclick");
        }
    }

}
