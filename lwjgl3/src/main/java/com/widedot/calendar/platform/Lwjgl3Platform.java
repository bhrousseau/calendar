package com.widedot.calendar.platform;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.Application.ApplicationType;

public class Lwjgl3Platform implements PlatformSpecific {
    private Json json;
    private FileHandle dataDir;

    @Override
    public void initialize() {
        json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        dataDir = Gdx.files.local("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }

    @Override
    public void dispose() {
        // Nothing to dispose
    }

    @Override
    public FileHandle getFile(String path) {
        return Gdx.files.internal(path);
    }

    @Override
    public void saveData(String key, String data) {
        FileHandle file = dataDir.child(key + ".json");
        file.writeString(data, false);
    }

    @Override
    public String loadData(String key) {
        FileHandle file = dataDir.child(key + ".json");
        if (file.exists()) {
            return file.readString();
        }
        return null;
    }

    @Override
    public boolean isTouchDevice() {
        return false;
    }

    @Override
    public void openURL(String url) {
        try {
            Gdx.net.openURI(url);
        } catch (Exception e) {
            Gdx.app.error("Lwjgl3Platform", "Error opening URL: " + url, e);
        }
    }

    @Override
    public void share(String text) {
        // Not implemented for desktop
    }

    @Override
    public void vibrate(int duration) {
        // Not implemented for desktop
    }

    @Override
    public void showKeyboard(boolean show) {
        // Not implemented for desktop
    }

    @Override
    public String getDeviceLanguage() {
        return Gdx.app.getPreferences("system").getString("language", "en");
    }

    @Override
    public String getDeviceModel() {
        return Gdx.app.getType().toString();
    }

    @Override
    public String getDeviceVersion() {
        return String.valueOf(Gdx.app.getVersion());
    }
} 