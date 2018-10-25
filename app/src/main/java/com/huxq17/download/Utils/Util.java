package com.huxq17.download.Utils;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Util {
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

    public static boolean deleteDir(File dirFile) {
        if (!dirFile.exists()) {
            return false;
        }
        if (dirFile.isFile()) {
            return dirFile.delete();
        } else {
            for (File file : dirFile.listFiles()) {
                deleteDir(file);
            }
        }
        return dirFile.delete();
    }

    public static void mergeFiles(File[] sources, File dest) {
        File[] sortedFiles = new File[sources.length];
        for (int i = 0; i < sources.length; i++) {
            File partFile = sources[i];
            String partFileName = partFile.getName();
            int idIndex = partFileName.lastIndexOf("-") + 1;
            int id = Integer.parseInt(partFileName.substring(idIndex));
            sortedFiles[id] = partFile;
        }
        BufferedOutputStream bufferedOutputStream = null;
        FileInputStream fileInputStream = null;
        try {
            if (!dest.exists()) {
                dest.createNewFile();
            }
            FileOutputStream outputStream = new FileOutputStream(dest);
//            bufferedOutputStream = new BufferedOutputStream(outputStream);
            byte[] buffer = new byte[8092];
            int len;
            for (int i = 0; i < sortedFiles.length; i++) {
                File file = sortedFiles[i];
                fileInputStream = new FileInputStream(file);
                while ((len = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }
                fileInputStream.close();
            }
//            bufferedOutputStream.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
//            closeQuietly(bufferedOutputStream);
            closeQuietly(fileInputStream);
        }
    }

    public static File getTempDir(String filePath) {
        File file = new File(filePath);
        File parentFile = file.getParentFile();
        File tempDir = new File(parentFile, "." + file.getName() + ".temp" + File.separatorChar);
        return tempDir;
    }
}
