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
find-rectangles.bat chemin/vers/image.png [mode]
```

Paramètres :
- `chemin/vers/image.png` : chemin vers l'image à analyser
- `mode` : mode d'organisation des rectangles (optionnel)
  - `columns` (par défaut) : groupe les rectangles par colonnes
  - `rows` : groupe les rectangles par lignes

#### Exemples :

```batch
# Mode colonnes (par défaut)
find-rectangles.bat image.png
find-rectangles.bat image.png columns

# Mode lignes
find-rectangles.bat image.png rows
```

Le fichier JSON sera créé au même endroit que l'image source, avec le même nom mais avec l'extension `.json`.

### Format de sortie JSON

#### Mode colonnes (par défaut)

```json
[
  {
    "col": 0,
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
- `col` : index de la colonne (0-based)
- `rectangles` : liste des rectangles dans la colonne, triés de bas en haut
- Pour chaque rectangle :
  - `x`, `y` : coordonnées du coin supérieur gauche
  - `width`, `height` : dimensions du rectangle

#### Mode lignes

```json
[
  {
    "row": 0,
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

- Chaque objet du tableau principal représente une ligne
- `row` : index de la ligne (0-based)
- `rectangles` : liste des rectangles dans la ligne, triés de gauche à droite
- Pour chaque rectangle :
  - `x`, `y` : coordonnées du coin supérieur gauche
  - `width`, `height` : dimensions du rectangle 