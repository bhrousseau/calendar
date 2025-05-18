package com.widedot.calendar.platform;

import com.badlogic.gdx.files.FileHandle;

public interface PlatformSpecific {
    void initialize();
    void dispose();
    FileHandle getFile(String path);
    void saveData(String key, String data);
    String loadData(String key);
    boolean isTouchDevice();
    void openURL(String url);
    void share(String text);
    void vibrate(int duration);
    void showKeyboard(boolean show);
    String getDeviceLanguage();
    String getDeviceModel();
    String getDeviceVersion();
} 