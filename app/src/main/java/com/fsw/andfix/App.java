package com.fsw.andfix;

import android.app.Application;

import com.fsw.andfixlibrary.AndFixManager;

/**
 * @author fsw
 * @version 1.0
 * @time 2017/4/17
 * @desc 测试项目的application
 */

public class App extends Application {

    public static AndFixManager manager;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            //初始化自定义热修复
            manager = new AndFixManager(this);
            // 加载所有修复的Dex包
            manager.loadFixDex();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
