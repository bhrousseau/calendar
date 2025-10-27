# Déploiement sur GitHub Pages

Ce projet utilise GitHub Actions pour déployer automatiquement la version HTML du jeu Calendar sur GitHub Pages.

## 🚀 Déploiement automatique

### Configuration initiale (à faire une seule fois)

1. **Activer GitHub Pages sur votre repository** :
   - Allez dans `Settings` > `Pages`
   - Dans `Source`, sélectionnez `Deploy from a branch`
   - Dans `Branch`, sélectionnez `gh-pages` et `/ (root)`
   - Cliquez sur `Save`

2. **Permissions pour GitHub Actions** :
   - Allez dans `Settings` > `Actions` > `General`
   - Dans `Workflow permissions`, sélectionnez `Read and write permissions`
   - Cochez `Allow GitHub Actions to create and approve pull requests`
   - Cliquez sur `Save`

### Déploiement automatique

Une fois configuré, **chaque push sur la branche `main`** déclenchera automatiquement :
1. La compilation du projet (`./gradlew :html:dist`)
2. Le déploiement des fichiers sur la branche `gh-pages`
3. La mise à jour de GitHub Pages

Le site sera accessible à : `https://<votre-username>.github.io/<nom-du-repo>/`

### Déploiement manuel via GitHub Actions

Vous pouvez aussi déclencher manuellement le déploiement :
1. Allez dans l'onglet `Actions` de votre repository
2. Sélectionnez le workflow `Deploy to GitHub Pages`
3. Cliquez sur `Run workflow`
4. Sélectionnez la branche `main`
5. Cliquez sur `Run workflow`

## 🔧 Build local

### Script automatisé (Windows)

Utilisez le script `deploy-local.bat` pour builder et préparer les fichiers :

```batch
.\deploy-local.bat
```

Ce script :
- Nettoie les builds précédents
- Compile la version HTML
- Copie les fichiers dans le dossier `deploy/`
- Crée le fichier `.nojekyll` nécessaire

### Build manuel

```batch
# Nettoyer
.\gradlew.bat :html:clean

# Compiler la version HTML optimisée
.\gradlew.bat :html:dist

# Les fichiers sont dans html\build\dist\
```

### Tester localement

Pour tester la version buildée avant de la déployer :

```batch
cd deploy
python -m http.server 8000
```

Puis ouvrez http://localhost:8000 dans votre navigateur.

## 📁 Structure des fichiers

```
calendar/
├── .github/
│   └── workflows/
│       └── deploy-gh-pages.yml  # Configuration GitHub Actions
├── html/
│   └── build/
│       └── dist/                # Fichiers compilés (générés)
├── deploy/                      # Copie locale pour tests (générée)
├── deploy-local.bat             # Script de build local
└── DEPLOYMENT.md                # Ce fichier
```

## 🔍 Vérification du déploiement

1. Allez dans l'onglet `Actions` de votre repository
2. Vérifiez que le workflow s'est exécuté avec succès (coche verte ✓)
3. Si erreur (croix rouge ✗), cliquez sur le workflow pour voir les logs
4. Attendez 2-3 minutes après un déploiement réussi
5. Testez votre site sur `https://<votre-username>.github.io/<nom-du-repo>/`

## ⚠️ Points importants

### Fichier `.nojekyll`
Le fichier `.nojekyll` est automatiquement créé pour éviter que GitHub Pages ignore les fichiers commençant par underscore (`_`), ce qui est nécessaire pour les assets GWT.

### Branche `gh-pages`
- **NE PAS** modifier manuellement cette branche
- Elle est automatiquement créée et mise à jour par GitHub Actions
- Contient uniquement les fichiers buildés, pas le code source

### Gitignore
Le dossier `deploy/` est local et ne doit pas être commité. Ajoutez-le à `.gitignore` :

```
/deploy/
```

## 🐛 Résolution de problèmes

### Le site ne s'affiche pas
- Vérifiez que GitHub Pages est activé dans les Settings
- Vérifiez que la branche `gh-pages` existe
- Attendez quelques minutes après le déploiement
- Videz le cache de votre navigateur (Ctrl+F5)

### Erreur de compilation dans GitHub Actions
- Vérifiez que le build fonctionne localement : `.\gradlew.bat :html:dist`
- Vérifiez les logs dans l'onglet Actions
- Assurez-vous que toutes les dépendances sont dans le repository

### Assets manquants
- Vérifiez que tous les fichiers d'assets sont dans le dossier `assets/`
- Vérifiez que le fichier `.nojekyll` est présent dans le déploiement
- Vérifiez les chemins d'accès aux assets (ils doivent être relatifs)

### Permissions refusées
- Vérifiez les permissions dans `Settings` > `Actions` > `General`
- Assurez-vous que `Read and write permissions` est activé

## 📝 Workflow du développement

1. Développez et testez localement avec `.\run-html.bat`
2. Committez vos changements sur `main`
3. Poussez sur GitHub
4. GitHub Actions déploie automatiquement
5. Vérifiez le site en production après 2-3 minutes

## 🔄 Mise à jour du site

Pour mettre à jour le site déployé :
1. Faites vos modifications
2. Committez et poussez sur `main`
3. C'est tout ! Le site sera mis à jour automatiquement.

