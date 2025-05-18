# Guide de Migration vers une Architecture Multi-Plateforme LibGDX

## Table des matières
1. [Introduction](#introduction)
2. [Prérequis](#prérequis)
3. [Structure du Projet](#structure-du-projet)
4. [Étapes de Migration](#étapes-de-migration)
5. [Bonnes Pratiques](#bonnes-pratiques)
6. [Dépannage](#dépannage)

## Introduction

Ce guide détaille le processus de migration du projet Calendar vers une architecture multi-plateforme LibGDX, supportant à la fois LWJGL3 et HTML/GWT. L'objectif est de créer une base de code unique qui fonctionne de manière optimale sur les deux plateformes tout en minimisant la duplication de code.

## Prérequis

- LibGDX 1.12.0 ou supérieur
- Java 8 ou supérieur
- Gradle 7.0 ou supérieur
- Un IDE compatible (IntelliJ IDEA recommandé)

## Structure du Projet

### Structure Recommandée
```
project/
├── core/                 # Code commun à toutes les plateformes
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   └── test/
├── lwjgl3/              # Implémentation spécifique LWJGL3
│   └── src/
│       └── main/
│           └── java/
├── html/                # Nouveau module pour la version HTML
│   └── src/
│       └── main/
│           ├── java/
│           └── webapp/
├── assets/              # Ressources partagées
└── build.gradle         # Configuration principale
```

## Étapes de Migration

### 1. Configuration du Projet

#### 1.1 Mise à jour du build.gradle principal
```gradle
// Ajouter dans settings.gradle
include 'html'

// Dans le build.gradle principal
project(":html") {
    apply plugin: "gwt"
    apply plugin: "war"

    dependencies {
        implementation project(":core")
        implementation "com.badlogicgames.gdx:gdx-backend-gwt:$gdxVersion"
        implementation "com.badlogicgames.gdx:gdx:$gdxVersion:sources"
        implementation "com.badlogicgames.gdx:gdx-backend-gwt:$gdxVersion:sources"
    }
}
```

#### 1.2 Configuration GWT
- Créer le fichier `html/src/main/java/com/widedot/calendar/GwtDefinition.gwt.xml`
- Configurer les modules GWT nécessaires

### 2. Création des Abstractions

#### 2.1 Interface PlatformSpecific
```java
public interface PlatformSpecific {
    void initialize();
    void dispose();
    FileHandle getFile(String path);
    void saveData(String key, String data);
    String loadData(String key);
    // Autres méthodes spécifiques à la plateforme
}
```

#### 2.2 Implémentations par Plateforme
```java
// Dans lwjgl3/
public class Lwjgl3Platform implements PlatformSpecific {
    // Implémentation LWJGL3
}

// Dans html/
public class HtmlPlatform implements PlatformSpecific {
    // Implémentation HTML
}
```

### 3. Remplacement des API Java Standard

#### 3.1 Gestion des Fichiers
```java
// Avant
File file = new File("path/to/file");

// Après
FileHandle file = Gdx.files.internal("path/to/file");
```

#### 3.2 Collections
```java
// Avant
List<String> list = new ArrayList<>();
Map<String, Object> map = new HashMap<>();

// Après
Array<String> list = new Array<>();
ObjectMap<String, Object> map = new ObjectMap<>();
```

#### 3.3 Gestion des Images
```java
// Avant
BufferedImage image = ImageIO.read(new File("image.png"));

// Après
Pixmap pixmap = new Pixmap(Gdx.files.internal("image.png"));
```

#### 3.4 Gestion du Temps
```java
// Avant
Calendar calendar = Calendar.getInstance();

// Après
long currentTime = TimeUtils.millis();
```

### 4. Gestion des Ressources

#### 4.1 Configuration de l'AssetManager
```java
public class AssetManager {
    private static com.badlogic.gdx.assets.AssetManager instance;
    
    public static void initialize() {
        instance = new com.badlogic.gdx.assets.AssetManager();
        // Configuration des loaders
    }
    
    public static void loadAssets() {
        // Chargement des ressources
    }
}
```

#### 4.2 Organisation des Assets
- Déplacer tous les assets dans le dossier `assets/`
- Utiliser des chemins relatifs
- Créer un fichier de configuration pour les assets

### 5. Gestion des Entrées

#### 5.1 Abstraction des Contrôles
```java
public interface InputHandler {
    boolean isKeyPressed(int key);
    boolean isTouchDown(int screenX, int screenY);
    // Autres méthodes d'entrée
}
```

#### 5.2 Implémentation par Plateforme
- Adapter les contrôles pour chaque plateforme
- Gérer les différences entre souris et tactile

## Bonnes Pratiques

### 1. Organisation du Code
- Séparer clairement le code commun du code spécifique à la plateforme
- Utiliser des interfaces pour les fonctionnalités spécifiques
- Implémenter le pattern Factory pour la création d'instances

### 2. Gestion des Ressources
- Utiliser l'AssetManager pour toutes les ressources
- Implémenter un système de préchargement
- Gérer correctement la libération des ressources

### 3. Performance
- Utiliser les collections LibGDX pour de meilleures performances
- Implémenter le pooling d'objets
- Optimiser le chargement des ressources

### 4. Tests
- Créer des tests unitaires pour le code commun
- Tester sur les deux plateformes
- Implémenter des tests d'intégration

## Dépannage

### Problèmes Courants

#### 1. Erreurs GWT
- Vérifier la configuration GWT
- S'assurer que toutes les classes sont compatibles
- Vérifier les dépendances

#### 2. Problèmes de Performance
- Utiliser le profiler pour identifier les goulots d'étranglement
- Optimiser le chargement des ressources
- Implémenter le lazy loading

#### 3. Problèmes de Compatibilité
- Vérifier les API utilisées
- Remplacer les API non compatibles
- Utiliser les abstractions LibGDX

### Solutions Recommandées

1. **Pour les erreurs de compilation GWT**
   - Vérifier le fichier GwtDefinition.gwt.xml
   - S'assurer que toutes les classes sont incluses
   - Vérifier les dépendances

2. **Pour les problèmes de performance**
   - Utiliser le pooling d'objets
   - Optimiser le chargement des ressources
   - Implémenter le lazy loading

3. **Pour les problèmes de compatibilité**
   - Utiliser les abstractions LibGDX
   - Implémenter des wrappers pour les API spécifiques
   - Tester régulièrement sur les deux plateformes

## Conclusion

Cette migration permettra d'avoir une base de code unique et maintenable pour les deux plateformes. Suivez les étapes dans l'ordre et testez régulièrement pour assurer une transition en douceur.

## Ressources Additionnelles

- [Documentation LibGDX](https://libgdx.com/wiki/)
- [Guide GWT](https://www.gwtproject.org/doc/latest/DevGuide.html)
- [Forum LibGDX](https://badlogicgames.com/forum/) 