# Outils Java

Ce dossier contient des outils Java utilitaires pour le projet.

## Prérequis

- Java Development Kit (JDK) 11 ou supérieur
- Les variables d'environnement JAVA_HOME et PATH doivent être correctement configurées

## Structure du projet

```
tools/
  ├── src/                    # Code source
  │   └── main/java/
  │       └── com/widedot/tools/
  │           └── BlackRectangleFinder.java
  ├── bin/                    # Fichiers compilés (créé automatiquement)
  ├── build.bat              # Script de compilation
  ├── find-rectangles.bat    # Script d'exécution
  └── README.md              # Ce fichier
```

## Compilation

Pour compiler tous les outils :

```batch
build.bat
```

## Utilisation

### BlackRectangleFinder

Cet outil analyse une image PNG pour trouver les rectangles noirs et génère un fichier JSON avec leurs positions.

#### Utilisation avec l'image par défaut :

```batch
find-rectangles.bat
```

#### Utilisation avec une image spécifique :

```batch
find-rectangles.bat chemin/vers/image.png
```

Le fichier JSON sera créé au même endroit que l'image source, avec le même nom mais avec l'extension `.json`.

### Format de sortie JSON

```json
[
  {
    "x": 100,
    "rectangles": [
      {
        "x": 100,
        "y": 500,
        "width": 30,
        "height": 30
      }
    ]
  }
]
```

- Chaque objet du tableau principal représente une colonne
- `x` : position horizontale de la colonne
- `rectangles` : liste des rectangles dans la colonne, triés de bas en haut
- Pour chaque rectangle :
  - `x`, `y` : coordonnées du coin supérieur gauche
  - `width`, `height` : dimensions du rectangle 