package com.huxq17.download.action;

import com.huxq17.download.ErrorCode;
import com.huxq17.download.TransferInfo;
import com.huxq17.download.task.DownloadTask;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GetContentLengthAction implements Action {
    @Override
    public boolean proceed(DownloadTask t) {
        boolean result = true;
        TransferInfo transferInfo = t.getDownloadInfo();
        HttpURLConnection conn = null;
        try {
            URL httpUrl = new URL(transferInfo.getUrl());
            conn = (HttpURLConnection) httpUrl.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setRequestMethod("HEAD");
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            if (conn.getResponseCode() == 302) {
                httpUrl = new URL(conn.getHeaderField("location"));
                conn = (HttpURLConnection) httpUrl.openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setRequestMethod("HEAD");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
            }
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Map<String, List<String>> headers = conn.getHeaderFields();
                Set<Map.Entry<String, List<String>>> sets = headers.entrySet();
                for (Map.Entry<String, List<String>> entry : sets) {
                    String key = entry.getKey();
                    if (entry.getValue() != null)
                        for (String value : entry.getValue()) {
//                        Log.e("tag", "head key=" + key + ";value=" + value);
                        }
                }
                String contentLengthStr = conn.getHeaderField("content-length");
                transferInfo.setContentLength(Long.parseLong(contentLengthStr));
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                result = false;
                transferInfo.setErrorCode(ErrorCode.FILE_NOT_FOUND);
            } else {
                result = false;
                transferInfo.setErrorCode(ErrorCode.UNKNOWN_SERVER_ERROR);
            }

        } catch (IOException e) {
            e.printStackTrace();
            transferInfo.setErrorCode(ErrorCode.NETWORK_UNAVAILABLE);
            result = false;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            //TODO 此时应该开启非断点下载
            transferInfo.setErrorCode(ErrorCode.CONTENT_LENGTH_NOT_FOUND);
            result = false;
        } finally {
            conn.disconnect();
        }
        return result;
    }
}
