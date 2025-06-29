# Mini-jeu Questions et Réponses

## Description

Le mini-jeu Questions et Réponses permet aux joueurs de répondre à des questions en tapant leur réponse au clavier. Ce jeu s'intègre parfaitement dans la structure existante du calendrier de l'avent.

## Fonctionnalités

- **Questions configurables** : Les questions sont chargées depuis des fichiers JSON
- **Réponses multiples** : Chaque question peut avoir plusieurs réponses acceptées
- **Saisie clavier** : Le joueur tape sa réponse directement au clavier
- **Feedback visuel** : Affichage des bonnes/mauvaises réponses avec des couleurs
- **Sons** : Effets sonores pour les bonnes et mauvaises réponses
- **Thèmes personnalisables** : Couleurs et apparence configurables via les paramètres

## Configuration

### Template de jeu (gameTemplates.json)

```json
"QNA": {
  "name": "questionAnswer",
  "defaultParameters": {
    "questionsFile": "questions.json",
    "caseSensitive": false,
    "bgColor": "25,25,50",
    "textColor": "255,255,255"
  },
  "parameterTypes": {
    "questionsFile": "string",
    "caseSensitive": "boolean",
    "bgColor": "string",
    "textColor": "string"
  },
  "presets": {
    "caseSensitive": {
      "caseSensitive": true
    },
    "darkTheme": {
      "bgColor": "10,10,20",
      "textColor": "200,200,255"
    },
    "lightTheme": {
      "bgColor": "240,240,250",
      "textColor": "50,50,100"
    }
  }
}
```

### Paramètres disponibles

- **questionsFile** : Nom du fichier JSON contenant les questions
- **caseSensitive** : Si true, la casse est prise en compte pour les réponses
- **bgColor** : Couleur de fond au format "R,G,B"
- **textColor** : Couleur du texte au format "R,G,B"

### Format des questions (JSON)

```json
{
  "questions": [
    {
      "question": "Quelle est la capitale de la France ?",
      "answer": "Paris",
      "alternatives": ["paris", "PARIS"]
    },
    {
      "question": "Combien font 2 + 2 ?",
      "answer": "4",
      "alternatives": ["quatre", "Quatre"]
    }
  ]
}
```

### Structure des questions

- **question** : Le texte de la question à afficher
- **answer** : La réponse principale acceptée
- **alternatives** : (optionnel) Tableau des réponses alternatives acceptées

## Utilisation dans games.json

```json
{
  "reference": "QNA_general_knowledge",
  "gameTemplate": "QNA",
  "theme": "composition_1916",
  "parameters": {
    "questionsFile": "questions.json"
  }
}
```

## Exemples de configurations

### Configuration basique
```json
{
  "reference": "QNA_basic",
  "gameTemplate": "QNA",
  "theme": "starry_night"
}
```

### Configuration avec sensibilité à la casse
```json
{
  "reference": "QNA_case_sensitive",
  "gameTemplate": "QNA",
  "theme": "guernica",
  "parameters": {
    "caseSensitive": true
  },
  "presets": ["darkTheme"]
}
```

### Configuration avec fichier de questions personnalisé
```json
{
  "reference": "QNA_art",
  "gameTemplate": "QNA",
  "theme": "mona_lisa",
  "parameters": {
    "questionsFile": "art_questions.json"
  }
}
```

## Contrôles

- **Clavier** : Tapez votre réponse
- **Entrée** : Valider la réponse
- **Retour arrière** : Effacer des caractères
- **Bouton "Retour"** : Retourner au calendrier
- **Bouton "Valider"** : Valider la réponse (alternative à Entrée)

## Intégration

Le mini-jeu s'intègre automatiquement dans le système existant :

1. **GameScreen** : Hérite de la classe de base `GameScreen`
2. **DynamicGameScreenFactory** : Enregistré dans le registre des jeux
3. **Configuration JSON** : Utilise le système de templates et paramètres existant
4. **Thèmes** : Compatible avec le système de thèmes visuels existant

## Fichiers créés/modifiés

### Nouveaux fichiers :
- `core/src/main/java/com/widedot/calendar/screens/QuestionAnswerGameScreen.java`
- `assets/questions.json`
- `assets/art_questions.json`
- `assets.reference/questions.json`
- `assets.reference/art_questions.json`

### Fichiers modifiés :
- `core/src/main/java/com/widedot/calendar/game/DynamicGameScreenFactory.java`
- `assets/gameTemplates.json`
- `assets/games.json`
- `assets.reference/gameTemplates.json`
- `assets.reference/games.json` 