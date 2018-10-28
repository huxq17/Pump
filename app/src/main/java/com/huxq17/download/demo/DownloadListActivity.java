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

import com.huxq17.download.DownloadInfo;
import com.huxq17.download.Pump;
import com.huxq17.download.listener.DownloadObserver;

import java.util.List;

public class DownloadListActivity extends AppCompatActivity {
    DownloadObserver downloadObserver = new DownloadObserver() {
        @Override
        public void onProgressUpdate(int progress) {
            DownloadInfo downloadInfo = getDownloadInfo();
            Log.e("main", "Main progress=" + progress + ";filePath=" + downloadInfo.filePath);
        }

        @Override
        public void onError(int errorCode) {

        }
    };
    private RecyclerView recyclerView;
    private DownloadAdapter downloadAdapter;
    private List<DownloadInfo> downloadInfoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Pump.subscribe(downloadObserver);
        recyclerView = findViewById(R.id.rvDownloadList);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        downloadInfoList = Pump.getAllDownloadList();
        recyclerView.setLayoutManager(linearLayoutManager);
        downloadAdapter = new DownloadAdapter(downloadInfoList);
        recyclerView.setAdapter(downloadAdapter);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Pump.unSubscribe(downloadObserver);
    }

    public static class DownloadAdapter extends RecyclerView.Adapter<DownloadViewHolder> {
        List<DownloadInfo> downloadInfoList;

        public DownloadAdapter(List<DownloadInfo> downloadInfoList) {
            this.downloadInfoList = downloadInfoList;
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
            int progress = downloadInfo.progress;
            viewHolder.progressBar.setProgress(progress);
        }

        @Override
        public int getItemCount() {
            return downloadInfoList.size();
        }
    }

    public static class DownloadViewHolder extends RecyclerView.ViewHolder {
        ProgressBar progressBar;

        public DownloadViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.pb_progress);
        }
    }

}
