# Solution Finale - DEVELOPER_ERROR

## Diagnostic
L'erreur `DEVELOPER_ERROR` (code 10) persiste, ce qui signifie que **la configuration dans Google Cloud Console ne correspond PAS à votre application Android**.

## Solution : Créer un nouveau projet Google Cloud (RECOMMANDÉ)

### Étape 1 : Créer un nouveau projet

1. Allez sur https://console.cloud.google.com/
2. Cliquez sur le sélecteur de projet (en haut)
3. Cliquez sur **NEW PROJECT**
4. **Project name**: `WayFinder Auth` (ou autre nom)
5. Cliquez sur **Create**
6. Attendez que le projet soit créé
7. **Sélectionnez le nouveau projet**

### Étape 2 : Activer l'API Google Sign-In

1. Allez dans **APIs & Services** > **Library**
2. Recherchez **"Google Sign-In API"**
3. Si vous ne trouvez pas, recherchez **"Identity Toolkit API"**
4. Cliquez dessus
5. Cliquez sur **Enable**
6. Attendez 1-2 minutes

### Étape 3 : Configurer OAuth Consent Screen

1. Allez dans **APIs & Services** > **OAuth consent screen**
2. **User Type**: Sélectionnez **External** (ou Internal si vous avez G Suite)
3. Cliquez sur **Create**
4. **App name**: `WayFinder`
5. **User support email**: Votre email
6. **App logo**: (optionnel, peut laisser vide)
7. **Application home page**: `https://wayfinder.com` (ou autre URL)
8. **Application privacy policy link**: (optionnel)
9. **Application terms of service link**: (optionnel)
10. **Authorized domains**: Laissez vide pour le moment
11. **Developer contact information**: Votre email
12. Cliquez sur **Save and Continue**
13. **Scopes**: Cliquez sur **Save and Continue** (les scopes par défaut suffisent)
14. **Test users**: 
    - Si vous êtes en mode Testing, cliquez sur **+ ADD USERS**
    - Ajoutez votre email Google
    - Cliquez sur **Add**
    - Cliquez sur **Save and Continue**
15. Cliquez sur **Back to Dashboard**

### Étape 4 : Créer le OAuth Client ID Android

1. Allez dans **APIs & Services** > **Credentials**
2. Cliquez sur **+ CREATE CREDENTIALS** > **OAuth client ID**
3. Si c'est la première fois, vous serez redirigé vers OAuth consent screen (suivez l'étape 3)
4. **Application type**: Sélectionnez **Android**
5. **Name**: `WayFinder Android Debug` (ou autre nom)
6. **Package name**: 
   ```
   tn.esprit.WayFinder
   ```
   ⚠️ **COPIEZ-COLLEZ EXACTEMENT** (W et F majuscules, pas d'espaces)
7. **SHA-1 certificate fingerprint**:
   ```
   9A:4B:00:51:E4:0B:AB:8B:D9:DB:53:42:DD:15:A6:84:48:F7:3D:94
   ```
   ⚠️ **COPIEZ-COLLEZ EXACTEMENT** (avec deux-points, majuscules, pas d'espaces)
8. Cliquez sur **Create**
9. **COPIEZ le Client ID** généré (format: `xxxxx-xxxxx.apps.googleusercontent.com`)

### Étape 5 : Mettre à jour strings.xml

1. Ouvrez `android/app/src/main/res/values/strings.xml`
2. Remplacez la ligne du Client ID :
   ```xml
   <string name="google_client_id_android">VOTRE_NOUVEAU_CLIENT_ID_ICI</string>
   ```
3. **Sauvegardez** (Cmd+S)

### Étape 6 : Mettre à jour Render.com (Backend)

1. Allez sur votre dashboard Render.com
2. Sélectionnez votre service backend
3. Allez dans **Environment**
4. Trouvez la variable `GOOGLE_CLIENT_ID_ANDROID`
5. **Mettez à jour** avec le nouveau Client ID
6. Si elle n'existe pas, **ajoutez-la**:
   - Key: `GOOGLE_CLIENT_ID_ANDROID`
   - Value: `VOTRE_NOUVEAU_CLIENT_ID_ICI`
7. **Sauvegardez**

### Étape 7 : Attendre la propagation

⏰ **ATTENDEZ 10-15 MINUTES** après avoir créé le Client ID dans Google Cloud Console.
Les changements peuvent prendre du temps à se propager.

### Étape 8 : Désinstaller complètement l'application

1. **Arrêtez l'application** dans l'émulateur
2. **Désinstallez complètement**:
   - Long press sur l'icône WayFinder > **Désinstaller**
   - Ou: Settings > Apps > WayFinder > **Uninstall**
3. **Redémarrez l'émulateur** (recommandé)

### Étape 9 : Clean et Rebuild

1. Dans Android Studio:
   - **Build** > **Clean Project**
   - Attendez que le clean se termine
   - **Build** > **Rebuild Project**
   - Attendez que le rebuild se termine (vérifiez qu'il n'y a pas d'erreurs)

### Étape 10 : Tester

1. **Run** l'application
2. Cliquez sur le bouton **Google**
3. L'écran Google Sign-In devrait s'ouvrir
4. **Sélectionnez un compte Google**
5. Si vous pouvez vous connecter → **Succès !** ✅

---

## Si ça ne fonctionne TOUJOURS pas

### Vérification ultime

1. **Vérifiez le package name** dans `build.gradle.kts` ligne 13:
   ```kotlin
   applicationId = "tn.esprit.WayFinder"  // Doit être EXACTEMENT comme ça
   ```

2. **Vérifiez le SHA-1** à nouveau:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
   ```
   Doit afficher: `9A:4B:00:51:E4:0B:AB:8B:D9:DB:53:42:DD:15:A6:84:48:F7:3D:94`

3. **Vérifiez dans Google Cloud Console**:
   - Le projet sélectionné est le bon
   - Le Client ID correspond exactement à celui dans `strings.xml`
   - Le package name dans le Client ID est `tn.esprit.WayFinder` (exactement)
   - Le SHA-1 dans le Client ID est `9A:4B:00:51:E4:0B:AB:8B:D9:DB:53:42:DD:15:A6:84:48:F7:3D:94` (exactement)

### Alternative : Utiliser un release keystore

Si le problème persiste, essayez avec un release keystore, mais pour le développement, le debug keystore devrait fonctionner.

---

## Résumé des valeurs EXACTES à copier-coller

**Dans Google Cloud Console > OAuth Client ID Android :**

```
Package name:
tn.esprit.WayFinder

SHA-1:
9A:4B:00:51:E4:0B:AB:8B:D9:DB:53:42:DD:15:A6:84:48:F7:3D:94
```

**Dans strings.xml :**

```xml
<string name="google_client_id_android">VOTRE_CLIENT_ID_DU_NOUVEAU_PROJET</string>
```

---

## Checklist finale avant de tester

- [ ] Nouveau projet créé dans Google Cloud Console
- [ ] API Google Sign-In activée
- [ ] OAuth Consent Screen configuré
- [ ] OAuth Client ID Android créé avec les valeurs exactes
- [ ] strings.xml mis à jour avec le nouveau Client ID
- [ ] Render.com mis à jour avec le nouveau Client ID (si nécessaire)
- [ ] Attendu 10-15 minutes
- [ ] Application complètement désinstallée
- [ ] Clean & Rebuild effectué
- [ ] Test effectué

Si toutes ces étapes sont complétées et que ça ne fonctionne toujours pas, le problème pourrait être lié à un problème avec Google Play Services sur l'émulateur ou une autre configuration système.

