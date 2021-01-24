package com.huxq17.download.demo.remote;

import com.huxq17.download.demo.DemoApplication;
import com.huxq17.download.utils.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedHashSet;

public class DownloadingTaskStore {
    private static final String FILE_NAME = "download_list_cf.txt";

    public static void storeDownloadingList(LinkedHashSet<String> downloadingList) {
        File cf = new File(Util.getPumpCachePath(DemoApplication.getInstance()), FILE_NAME);
        ObjectOutputStream objectOutputStream = null;
        try {
            if (cf.exists() || cf.createNewFile()) {
                objectOutputStream = new ObjectOutputStream(new FileOutputStream(cf));
                objectOutputStream.writeObject(downloadingList);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Util.closeQuietly(objectOutputStream);
        }
    }

    public static LinkedHashSet<String> restoreDownloadingList() {
        LinkedHashSet<String> result = null;
        File cf = new File(Util.getPumpCachePath(DemoApplication.getInstance()), FILE_NAME);
        ObjectInputStream objectInputStream = null;
        try {
            if (cf.exists() || cf.createNewFile()) {
                objectInputStream = new ObjectInputStream(new FileInputStream(cf));
                result = (LinkedHashSet<String>) objectInputStream.readObject();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            Util.closeQuietly(objectInputStream);
        }
        return result == null ? new LinkedHashSet<String>() : result;
    }

}
