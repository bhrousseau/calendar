package com.widedot.calendar.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire de ressources pour le jeu
 * Charge et gère les textures, sons, polices, etc.
 */
public class ResourceManager {
    private static ResourceManager instance;
    
    private final Map<String, Texture> textures;
    private final Map<String, Sound> sounds;
    private final Map<String, Music> music;
    private final Map<String, BitmapFont> fonts;
    private final Map<String, TextureAtlas> textureAtlases;
    
    /**
     * Constructeur privé pour le pattern Singleton
     */
    private ResourceManager() {
        textures = new HashMap<>();
        sounds = new HashMap<>();
        music = new HashMap<>();
        fonts = new HashMap<>();
        textureAtlases = new HashMap<>();
    }
    
    /**
     * Récupère l'instance unique de ResourceManager (pattern Singleton)
     * @return L'instance de ResourceManager
     */
    public static ResourceManager getInstance() {
        if (instance == null) {
            instance = new ResourceManager();
        }
        return instance;
    }
    
    /**
     * Charge une texture depuis un fichier
     * @param path Chemin vers le fichier de texture
     * @return La texture chargée
     */
    public Texture getTexture(String path) {
        if (!textures.containsKey(path)) {
            textures.put(path, new Texture(Gdx.files.internal(path)));
        }
        return textures.get(path);
    }
    
    /**
     * Charge un son depuis un fichier
     * @param path Chemin vers le fichier de son
     * @return Le son chargé
     */
    public Sound getSound(String path) {
        if (!sounds.containsKey(path)) {
            sounds.put(path, Gdx.audio.newSound(Gdx.files.internal(path)));
        }
        return sounds.get(path);
    }
    
    /**
     * Charge une musique depuis un fichier
     * @param path Chemin vers le fichier de musique
     * @return La musique chargée
     */
    public Music getMusic(String path) {
        if (!music.containsKey(path)) {
            music.put(path, Gdx.audio.newMusic(Gdx.files.internal(path)));
        }
        return music.get(path);
    }
    
    /**
     * Charge une police depuis un fichier
     * @param path Chemin vers le fichier de police
     * @return La police chargée
     */
    public BitmapFont getFont(String path) {
        if (!fonts.containsKey(path)) {
            fonts.put(path, new BitmapFont(Gdx.files.internal(path)));
        }
        return fonts.get(path);
    }
    
    /**
     * Charge un atlas de textures depuis un fichier
     * @param path Chemin vers le fichier d'atlas
     * @return L'atlas chargé
     */
    public TextureAtlas getTextureAtlas(String path) {
        if (!textureAtlases.containsKey(path)) {
            textureAtlases.put(path, new TextureAtlas(Gdx.files.internal(path)));
        }
        return textureAtlases.get(path);
    }
    
    /**
     * Libère toutes les ressources
     */
    public void dispose() {
        for (Texture texture : textures.values()) {
            texture.dispose();
        }
        textures.clear();
        
        for (Sound sound : sounds.values()) {
            sound.dispose();
        }
        sounds.clear();
        
        for (Music music : music.values()) {
            music.dispose();
        }
        music.clear();
        
        for (BitmapFont font : fonts.values()) {
            font.dispose();
        }
        fonts.clear();
        
        for (TextureAtlas atlas : textureAtlases.values()) {
            atlas.dispose();
        }
        textureAtlases.clear();
    }
} 