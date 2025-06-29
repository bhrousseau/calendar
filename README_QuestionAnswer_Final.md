# Mini-jeu QuestionAnswer - Phase Finale

## Vue d'ensemble

Le mini-jeu QuestionAnswer a été modifié pour inclure une **phase finale** qui valide la résolution de la case du calendrier. Après avoir répondu aux 10 questions de culture générale, le joueur doit identifier l'œuvre d'art spécifique du thème.

## Fonctionnement

### Phase 1 : Questions générales (10 questions)
- 10 questions de culture générale sélectionnées aléatoirement
- Progression avec jauge de 10 cercles (verts pour bonnes réponses, gris pour restantes)
- Score calculé sur 100% (pourcentage de bonnes réponses)

### Phase 2 : Question finale (validation de la case)
Après les 10 questions générales :

1. **Affichage de l'image du thème**
   - L'image du thème s'affiche clairement au centre de l'écran
   - Taille fixe de 250x250 pixels pour un affichage optimal

2. **Question spécifique au thème**
   - Question sur l'auteur, le titre ou l'année de l'œuvre
   - Questions personnalisées pour chaque thème via `theme_questions.json`
   - Réponses multiples acceptées (nom complet, nom de famille, variantes)

3. **Validation de la case**
   - Seule la réponse correcte à cette question finale valide la case
   - Les 10 questions générales servent à préparer le joueur pour identifier l'œuvre

## Configuration technique

### Nouveau fichier : `theme_questions.json`
```json
{
  "themeQuestions": {
    "nom_du_theme": {
      "question": "Qui a peint cette œuvre ?",
      "answer": "Nom de l'artiste",
      "alternatives": ["variante1", "variante2", "variante3"]
    }
  }
}
```

### Exemples de questions configurées
- **Guernica** : "Qui a peint Guernica ?" → "Pablo Picasso"
- **Mona Lisa** : "Qui est l'auteur de la Joconde ?" → "Leonardo da Vinci"
- **Starry Night** : "Qui a peint La Nuit étoilée ?" → "Vincent van Gogh"
- Et 21 autres œuvres...

## Modifications du code

### Nouvelles variables
- `finalQuestionPhase` : Indicateur de la phase finale
- `finalQuestion` : Question spécifique au thème
- `fullImageTexture` : Texture de l'image du thème

### Nouvelles méthodes
- `drawFinalPhase()` : Affichage de la phase finale
- `drawImageCentered()` : Affichage de l'image centrée
- `loadFinalQuestion()` : Chargement de la question finale
- `submitFinalAnswer()` : Validation de la réponse finale
- `completeFinalGame()` : Finalisation du jeu

### Calcul du score final
- **Questions générales** : 80% du score maximum (basé sur les bonnes réponses)
- **Question finale** : +20% si correcte
- **Total** : 100% maximum uniquement si question finale réussie

## Interface utilisateur

### Phase finale
- Image du thème affichée clairement (250x250px, centrée)
- Score des questions générales affiché en haut
- Question finale affichée clairement
- Zone de saisie pour identifier l'œuvre
- Instructions : "Identifiez cette œuvre d'art"

### Écran de fin (après validation)
- Titre : "Félicitations !"
- Récapitulatif du score des questions générales
- Message : "Œuvre correctement identifiée !"
- Image finale affichée sans flou (200x200px)

## Impact sur le gameplay

1. **Défi en deux phases** : Questions générales suivies de l'identification d'une œuvre d'art
2. **Validation finale** : La case n'est validée que si l'œuvre est correctement identifiée
3. **Éducatif** : Apprentissage de la culture générale ET de l'art
4. **Préparation** : Les questions générales préparent intellectuellement à l'identification finale

Cette modification transforme le mini-jeu en une expérience éducative complète, où la culture générale prépare à l'identification d'œuvres d'art célèbres. 