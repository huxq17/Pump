package com.huxq17.download.Utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Author: huxiaoqian
 * Version V1.0
 * Date: 2019/5/23
 * Description:
 * Modification History:
 * Date Author Version Description
 * -----------------------------------------------------------------------------------
 * 2019/5/23 huxiaoqian 1.0
 * Why & What is modified:
 */
public class ReflectUtil {
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<T> cls) {
        T instance = null;
        try {
            Constructor constructor = cls.getDeclaredConstructor();
            constructor.setAccessible(true);
            instance = (T) constructor.newInstance();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException("Do not do strange operation in the constructor.");
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    public static Object invokeMethod(Object targetObject, String methodName, Object[] params, Class[] paramTypes) {
        Object returnObj = null;
        if (targetObject == null || methodName == null || methodName.isEmpty()) {
            return null;
        }
        Method method = null;
        for (Class cls = targetObject.getClass(); cls != Object.class; cls = cls.getSuperclass()) {
            try {
                method = cls.getDeclaredMethod(methodName, paramTypes);
                break;
            } catch (Exception e) {
//                e.printStackTrace();
//                return null;
            }
        }
        if (method != null) {
            method.setAccessible(true);
            try {
                returnObj = method.invoke(targetObject, params);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return returnObj;
    }
}
