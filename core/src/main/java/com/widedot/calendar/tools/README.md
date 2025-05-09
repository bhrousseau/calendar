# ImagePositionFinder

Cet outil Java permet de retrouver la position d'une image secondaire (ex : vignette ou extrait carré) à l'intérieur d'une image principale (ex : peinture complète), en tenant compte d'une tolérance de différence de pixels. Il fonctionne aussi bien sur des fichiers individuels que sur des répertoires d'images.

## Fonctionnalités
- Recherche la position d'une image (ou d'un lot d'images) dans une autre image (ou lot d'images)
- Tolérance paramétrable pour accepter de petites différences de couleurs
- Affichage des logs détaillés sur stderr (progression, correspondances, erreurs)
- Résultat au format JSON sur stdout
- Optimisé pour de bonnes performances (recherche rapide puis affinée)

## Installation

1. Placez le fichier `ImagePositionFinder.java` dans le dossier `core/src/main/java/com/widedot/calendar/tools/` de votre projet.
2. Compilez le projet avec Gradle :
   ```bash
   ./gradlew clean compileJava
   ```

## Utilisation

### Lancement

```bash
java -cp core/build/classes/java/main com.widedot.calendar.tools.ImagePositionFinder <full_image_dir_or_file> <square_image_dir_or_file> [tolerance]
```

- `<full_image_dir_or_file>` : Chemin vers un dossier ou un fichier image principal (jpg/png)
- `<square_image_dir_or_file>` : Chemin vers un dossier ou un fichier image secondaire (jpg/png)
- `[tolerance]` (optionnel) : Tolérance d'erreur (entre 0 et 1, défaut : 0.1)

### Comportement
- Si vous indiquez un dossier, tous les fichiers images du dossier seront traités.
- Si vous indiquez un fichier, seul ce fichier sera traité.
- Les images sont appariées par nom de base (ex : `mon_image.jpg` avec `mon_image.jpg`)

### Exemples

**Pour traiter tous les fichiers d'un dossier :**
```bash
java -cp core/build/classes/java/main com.widedot.calendar.tools.ImagePositionFinder "C:/chemin/full" "C:/chemin/square"
```

**Pour traiter un seul fichier :**
```bash
java -cp core/build/classes/java/main com.widedot.calendar.tools.ImagePositionFinder "C:/chemin/full/mon_image.jpg" "C:/chemin/square/mon_image.jpg" 0.2
```

**Pour traiter un fichier contre tous les carrés d'un dossier :**
```bash
java -cp core/build/classes/java/main com.widedot.calendar.tools.ImagePositionFinder "C:/chemin/full/mon_image.jpg" "C:/chemin/square" 0.15
```

### Paramètres de tolérance
- Plus la tolérance est élevée, plus l'outil acceptera de différences de couleurs (ex : 0.6 = 60% d'erreur tolérée)
- Pour une recherche stricte, utilisez une valeur faible (ex : 0.05)

### Logs et sortie
- Les logs de progression et d'erreur sont affichés sur **stderr**
- Le résultat (positions trouvées, pourcentages de correspondance) est affiché au format **JSON** sur **stdout**

### Exemple de sortie JSON
```json
[
  {
    "fullImage": "mon_image.jpg",
    "squareImage": "mon_image.jpg",
    "x": 123,
    "y": 456,
    "width": 256,
    "height": 256,
    "matchPercentage": 0.9876
  }
]
```

## Dépendances
- Java 11 ou supérieur recommandé
- Utilise uniquement les bibliothèques standards Java (ImageIO, NIO, etc.)

## Conseils
- Pour de grandes images, ajustez la tolérance et soyez patient : l'algorithme est optimisé mais peut prendre du temps sur de très grands fichiers.
- Vérifiez les logs pour suivre la progression ou diagnostiquer d'éventuels problèmes.

---

**Auteur :** Projet java-games/calendar 