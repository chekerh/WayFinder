# Checklist pour résoudre DEVELOPER_ERROR (Code 10)

## ✅ Vérifications déjà faites

1. ✅ Package name dans `build.gradle.kts` : `tn.esprit.WayFinder`
2. ✅ SHA-1 : `9A:4B:00:51:E4:0B:AB:8B:D9:DB:53:42:DD:15:A6:84:48:F7:3D:94`
3. ✅ Client ID dans `strings.xml` : `825004975196-gr4rffsv0akatmmn9qonoqsq5aa4q5e6.apps.googleusercontent.com`
4. ✅ Configuration dans Google Cloud Console (package name et SHA-1 corrects)

## ❌ Vérifications à faire dans Google Cloud Console

### 1. API Google Sign-In activée ?

1. Allez sur https://console.cloud.google.com/
2. Sélectionnez le projet **WayFinderApp** (ou le projet qui contient votre Client ID)
3. Allez dans **APIs & Services** > **Library**
4. Recherchez **"Google Sign-In API"** ou **"Identity Toolkit API"**
5. Si l'API n'est pas activée, cliquez sur **Enable**
6. ⏰ **Attendez 2-5 minutes** après activation

### 2. OAuth Consent Screen configuré ?

1. Allez dans **APIs & Services** > **OAuth consent screen**
2. Vérifiez que :
   - **User Type** est sélectionné (External ou Internal)
   - **App name** est rempli
   - **User support email** est rempli
   - **Developer contact information** est rempli
3. Si ce n'est pas configuré, suivez les étapes :
   - Cliquez sur **Edit App**
   - Remplissez tous les champs obligatoires
   - Cliquez sur **Save and Continue**
   - Passez toutes les étapes (Scopes, Test users, etc.)
   - Cliquez sur **Back to Dashboard**

### 3. Client ID Android vérifié

1. Allez dans **APIs & Services** > **Credentials**
2. Trouvez le Client ID Android : `825004975196-gr4rffsv0akatmmn9qonoqsq5aa4q5e6`
3. Cliquez dessus pour voir les détails
4. Vérifiez que :
   - **Package name** : `tn.esprit.WayFinder` (exactement, avec W et F majuscules)
   - **SHA-1** : `9A:4B:00:51:E4:0B:AB:8B:D9:DB:53:42:DD:15:A6:84:48:F7:3D:94` (exactement)
5. Si quelque chose ne correspond pas, cliquez sur **Edit** et corrigez

### 4. Vérifier que le projet est le bon

1. Vérifiez que le Client ID iOS (`98193461257-de46aq85lh0t93uq4lq2o5ostvt400j6`) et le Client ID Android (`825004975196-gr4rffsv0akatmmn9qonoqsq5aa4q5e6`) sont dans le **même projet Google Cloud Console**
2. Si ce n'est pas le cas, vous devez soit :
   - Créer un nouveau Client ID Android dans le même projet que iOS
   - OU utiliser le même projet pour les deux

## 🔧 Actions à faire

### Étape 1 : Activer l'API Google Sign-In

```
Google Cloud Console > APIs & Services > Library > Rechercher "Google Sign-In API" > Enable
```

### Étape 2 : Configurer OAuth Consent Screen

```
Google Cloud Console > APIs & Services > OAuth consent screen > Edit App > Remplir tous les champs > Save
```

### Étape 3 : Attendre la propagation

⏰ **ATTENDEZ 10-15 MINUTES** après avoir activé l'API et configuré le OAuth Consent Screen.

### Étape 4 : Clean et Rebuild

1. Dans Android Studio :
   - **Build** > **Clean Project**
   - **Build** > **Rebuild Project**

### Étape 5 : Désinstaller complètement l'app

1. **Désinstallez** l'app de l'émulateur
2. **Redémarrez** l'émulateur (recommandé)

### Étape 6 : Tester

1. **Run** l'application
2. Cliquez sur le bouton **Google**
3. Vérifiez les logs dans Logcat pour voir les détails

## 📝 Logs à vérifier

Dans Logcat, cherchez :
- `GoogleSignInClient created successfully`
- `Client ID used: ...`
- `DEVELOPER_ERROR: Configuration OAuth incorrecte`

Si vous voyez toujours `DEVELOPER_ERROR`, vérifiez les 3 points ci-dessus dans Google Cloud Console.

## ⚠️ Points importants

1. **L'API Google Sign-In doit être activée** - C'est souvent la cause principale
2. **Le OAuth Consent Screen doit être configuré** - Même en mode Testing
3. **Attendez 10-15 minutes** après toute modification dans Google Cloud Console
4. **Les deux Client IDs (iOS et Android) doivent être dans le même projet**

## 🔍 Si ça ne fonctionne toujours pas

1. Vérifiez que vous êtes connecté avec le bon compte Google dans Google Cloud Console
2. Vérifiez que le projet Google Cloud Console est actif (pas suspendu)
3. Essayez de créer un nouveau Client ID Android dans le même projet
4. Vérifiez les logs détaillés dans Logcat (maintenant avec plus de détails)

