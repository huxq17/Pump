package com.huxq17.download;

import com.huxq17.download.Utils.LogUtil;

import java.util.HashMap;
import java.util.Map;

public class PumpFactory {
    private static Map<Class<?>,Object> serviceMap = new HashMap<>();
    public static <T> T getService(Class<T> tClass){
        return (T) serviceMap.get(tClass);
    }
    public static void addService(Class<?> serviceClass,Object service){
        serviceMap.put(serviceClass,service);
    }
}
