# Guide pour corriger les erreurs de build

## Erreurs identifiées

1. **"Missing package product 'GoogleSignIn'"** - Package Swift non résolu
2. **"Write() failed, errno=28"** - Espace disque plein
3. **"Linker command failed"** - Conséquence des erreurs précédentes

## Solutions étape par étape

### Étape 1 : Libérer de l'espace disque (PRIORITAIRE)

Le disque est plein (100% utilisé). Tu dois libérer au moins 5-10 Go avant de pouvoir builder.

**Actions rapides :**
1. Vider la corbeille
2. Nettoyer les téléchargements volumineux
3. Supprimer des fichiers temporaires :
   ```bash
   # Nettoyer les caches système
   sudo rm -rf ~/Library/Caches/*
   # Nettoyer les logs
   sudo rm -rf ~/Library/Logs/*
   ```

### Étape 2 : Réinitialiser les packages Swift dans Xcode

1. **Ouvre Xcode**
2. **File > Packages > Reset Package Caches**
3. **File > Packages > Resolve Package Versions**
4. Attends que les packages se téléchargent (peut prendre plusieurs minutes)

### Étape 3 : Nettoyer le build

1. Dans Xcode : **Product > Clean Build Folder** (⇧⌘K)
2. Ferme Xcode complètement
3. Relance Xcode et réessaie de builder

### Étape 4 : Si GoogleSignIn pose toujours problème (solution temporaire)

Si tu n'as pas besoin de Google Sign-In immédiatement, tu peux le désactiver temporairement :

1. Dans Xcode, va dans **Project Settings > Package Dependencies**
2. Désactive ou supprime temporairement GoogleSignIn-iOS
3. Le code utilise déjà `#if canImport(GoogleSignIn)` donc ça ne cassera pas l'app

### Étape 5 : Vérifier l'espace disque

```bash
df -h
```

Tu dois avoir au moins 5-10 Go libres pour que Xcode puisse builder correctement.

## Si les erreurs persistent

1. **Vérifie les erreurs spécifiques** dans le panneau Issues (⌘5)
2. **Partage les messages d'erreur exacts** pour que je puisse t'aider plus précisément
3. **Vérifie que le backend est accessible** - l'erreur API dans la console est normale si le backend n'est pas démarré

