package com.huxq17.download.message;

import android.content.Context;

import com.buyi.huxq17.serviceagency.annotation.ServiceAgent;
import com.huxq17.download.DownloadInfo;

import static com.huxq17.download.provider.Provider.CONTENT_URI;

@ServiceAgent
public class MessageCenter implements IMessageCenter {
    private Context context;

    @Override
    public void start(Context context) {
        this.context = context;
    }

    @Override
    public void notifyProgressChanged(DownloadInfo downloadInfo) {
        context.getContentResolver().notifyChange(CONTENT_URI, null);
    }
}
