package com.fsw.andfixlibrary;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import dalvik.system.BaseDexClassLoader;


/**
 * @author fsw
 * @version 1.0
 * @time 2017/4/17
 * @desc 热修复学习笔记
 */
public class AndFixManager {
    /*上下文*/
    private Context context;
    /*应用可访问的dex文件目录*/
    private File dexDir;

    public AndFixManager(Context context) {
        this.context = context;
        //获取应用可用的dex目录
        dexDir = context.getDir("dex", Context.MODE_PRIVATE);
    }

    /**
     * 修复dex包
     *
     * @param fixDexPath 修复包的路径
     */
    public void fixDex(String fixDexPath) throws IOException, NoSuchFieldException, IllegalAccessException {
        //获取下载好的补丁，并且移动到系统能够访问的dex目录下
        File srcFile = new File(fixDexPath);
        if (!srcFile.exists()) {
            throw new FileNotFoundException(fixDexPath);
        }
        File dexFile = new File(dexDir, srcFile.getName());
        //已存在说明已经修复过bug
        if (dexFile.exists()) {
            Log.d("AndFixManager", "patch [" + fixDexPath + "] has be loaded.");
            return;
        }
        //说明没有修复过此bug,将下载下来的dex文件拷贝进项目的dex目录
        copyFile(dexFile, srcFile);
        //ClassLoader读取FixDex路径，加入一个集合是因为已启动可能就要进行修复
        List<File> fixDexFiles = new ArrayList<>();
        fixDexFiles.add(dexFile);
        //修复bug
        fixDexFiles(fixDexFiles);
    }

    /*
     * 修复bug
     * @param fiDexFiles 需要修复的dex文件
     */
    private void fixDexFiles(List<File> fixDexFiles) throws NoSuchFieldException, IllegalAccessException {
        //获取已经运行的dexElements
        ClassLoader applicationClassLoader = context.getClassLoader();
        Object applicationDexElements = getElementsByClassLoader(applicationClassLoader);
        //获取解压路径
        File optimizedDir = new File(dexDir, "dex");
        if (!optimizedDir.exists()) {
            optimizedDir.mkdirs();
        }
        for (File fixDexFile : fixDexFiles) {
            ClassLoader fixDexClassLoader = new BaseDexClassLoader(
                    fixDexFile.getAbsolutePath(),// dex路径  必须要在应用目录下的dex文件中
                    optimizedDir,// 解压路径
                    null,// .so文件位置
                    applicationClassLoader// 父ClassLoader
            );
            Object fixDexElements = getElementsByClassLoader(fixDexClassLoader);
            //把要修复的dexElement插到已经运行的dexElement的最前面，合并
            applicationDexElements = combineArray(fixDexElements, applicationDexElements);
        }
        //把合并号的applicationDexElements注入到原来的applicationClassLoader中
        injectDexElements(applicationClassLoader, applicationDexElements);
    }

    /**
     * 将输入性注入到类中
     *
     * @param classLoader
     * @param dexElements
     */
    private void injectDexElements(ClassLoader classLoader, Object dexElements) throws NoSuchFieldException, IllegalAccessException {
        //先获取pathList
        Field pathListField = BaseDexClassLoader.class.getDeclaredField("pathList");
        pathListField.setAccessible(true);
        Object pathList = pathListField.get(classLoader);
        //pathList里面的dexElements
        Field dexElementsField = pathList.getClass().getDeclaredField("dexElements");
        dexElementsField.setAccessible(true);
        dexElementsField.set(pathList, dexElements);
    }

    /*
     * 合并两个数组
     * @param fixDexElements
     * @param applicationDexElements
     * @return
     */
    private Object combineArray(Object fixDexElements, Object applicationDexElements) {
        //反射获取数组的泛型
        Class<?> clazz = fixDexElements.getClass().getComponentType();
        int i = Array.getLength(fixDexElements);
        int j = i + Array.getLength(applicationDexElements);
        Object result = Array.newInstance(clazz, j);
        for (int k = 0; k < j; k++) {
            if (k < i) {
                Array.set(result, k, Array.get(fixDexElements, k));
            } else {
                Array.set(result, k, Array.get(applicationDexElements, k - i));
            }
        }
        return result;
    }

    /*
     *
     * @param classLoader
     * @return
     */
    private Object getElementsByClassLoader(ClassLoader classLoader) throws NoSuchFieldException, IllegalAccessException {
        //先获取pathList
        Field pathListField = BaseDexClassLoader.class.getDeclaredField("pathList");
        pathListField.setAccessible(true);
        Object pathList = pathListField.get(classLoader);
        //获取pathList里面的dexElements
        Field dexElementsField = pathList.getClass().getDeclaredField("dexElements");
        dexElementsField.setAccessible(true);
        return dexElementsField.get(pathList);
    }

    /*
     * 文件拷贝
     *
     * @param dexFile 目标文件
     * @param srcFile 临时文件
     * @throws IOException
     */
    private void copyFile(File dexFile, File srcFile) throws IOException {
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        if (!dexFile.exists()) {
            dexFile.createNewFile();
        }
        try {
            inChannel = new FileInputStream(srcFile).getChannel();
            outChannel = new FileOutputStream(dexFile).getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null) {
                inChannel.close();
            }
            if (outChannel != null) {
                outChannel.close();
            }
        }
    }

    /*
     * 加载全部的修复包
     */
    public void loadFixDex() throws NoSuchFieldException, IllegalAccessException {
        File[] dexFiles = dexDir.listFiles();
        List<File> fixDexFiles = new ArrayList<>();
        for (File dexFile : dexFiles) {
            if (dexFile.getName().endsWith(".dex")) {
                fixDexFiles.add(dexFile);
            }
        }
        fixDexFiles(fixDexFiles);
    }
}
