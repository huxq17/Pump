package com.huxq17.download.Utils;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

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

    public static boolean renameTo(File source, File dest) {
        if (dest.exists()) {
            if (dest.delete()) {
                return source.renameTo(dest);
            }
            return false;
        }
        return source.renameTo(dest);
    }

    public static void mergeFiles(File[] sources, File dest) {
        if (sources == null || sources.length <= 0) {
            return;
        }
        if (sources.length == 1) {
            renameTo(sources[0], dest);
            return;
        }
        File[] sortedFiles = new File[sources.length];
        for (int i = 0; i < sources.length; i++) {
            File partFile = sources[i];
            String partFileName = partFile.getName();
            int idIndex = partFileName.lastIndexOf("-") + 1;
            int id = Integer.parseInt(partFileName.substring(idIndex));
            sortedFiles[id] = partFile;
        }
        FileChannel destFileChannel = null;
        FileChannel inputChannel = null;
        try {
            destFileChannel = new FileOutputStream(sortedFiles[0], true).getChannel();
            for (int i = 1; i < sources.length; i++) {
                inputChannel = new FileInputStream(sources[i]).getChannel();
                destFileChannel.transferFrom(inputChannel, destFileChannel.size(), inputChannel.size());
                inputChannel.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(destFileChannel);
            closeQuietly(inputChannel);
        }
        renameTo(sortedFiles[0], dest);
    }

//    public static void mergeFiles(File[] sources, File dest) {
//        File[] sortedFiles = new File[sources.length];
//        for (int i = 0; i < sources.length; i++) {
//            File partFile = sources[i];
//            String partFileName = partFile.getName();
//            int idIndex = partFileName.lastIndexOf("-") + 1;
//            int id = Integer.parseInt(partFileName.substring(idIndex));
//            sortedFiles[id] = partFile;
//        }
//        FileInputStream fileInputStream = null;
//        try {
//            FileOutputStream outputStream = new FileOutputStream(sortedFiles[0],true);
//            byte[] buffer = new byte[8092];
//            int len;
//            for (int i = 1; i < sortedFiles.length; i++) {
//                File file = sortedFiles[i];
//                fileInputStream = new FileInputStream(file);
//                while ((len = fileInputStream.compute(buffer)) != -1) {
//                    outputStream.write(buffer, 0, len);
//                }
//                fileInputStream.close();
//            }
////            bufferedOutputStream.flush();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            closeQuietly(fileInputStream);
//        }
//        renameTo(sortedFiles[0], dest);
//    }

    public static File getTempDir(String filePath) {
        File file = new File(filePath);
        File parentFile = file.getParentFile();
        File tempDir = new File(parentFile, "." + file.getName() + ".temp" + File.separatorChar);
        return tempDir;
    }
}
