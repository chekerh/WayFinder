# Vérification de la Configuration Google OAuth

## Problème actuel
Le Google Sign-In s'ouvre mais retourne `RESULT_CANCELED` sans code d'erreur spécifique. Cela indique généralement un problème de configuration OAuth dans Google Cloud Console.

## Configuration requise dans Google Cloud Console

### 1. Informations de l'application
- **Package name**: `tn.esprit.WayFinder` (EXACTEMENT, attention à la casse)
- **SHA-1 fingerprint**: `9A:4B:00:51:E4:0B:AB:8B:D9:DB:53:42:DD:15:A6:84:48:F7:3D:94`
- **Client ID Android**: `825004975196-bovfg53oqaurlfmhehoeva4on629uc1e.apps.googleusercontent.com`

### 2. Étapes de vérification dans Google Cloud Console

1. **Allez sur** https://console.cloud.google.com/

2. **Sélectionnez votre projet** (ou créez-en un nouveau)

3. **Activez l'API Google Sign-In**:
   - Allez dans **APIs & Services** > **Library**
   - Recherchez "Google Sign-In API" ou "Identity Toolkit API"
   - Cliquez sur **Enable** si ce n'est pas déjà fait

4. **Vérifiez les Credentials**:
   - Allez dans **APIs & Services** > **Credentials**
   - Trouvez votre OAuth 2.0 Client ID (type: Android)
   - Cliquez dessus pour l'éditer

5. **Vérifiez les champs suivants**:
   - **Name**: Peut être n'importe quel nom (ex: "WayFinder Android")
   - **Package name**: DOIT être exactement `tn.esprit.WayFinder`
     - ⚠️ **ATTENTION**: La casse est importante !
     - Ne doit pas contenir d'espaces
     - Ne doit pas avoir de majuscules autres que celles indiquées
   - **SHA-1 certificate fingerprint**: 
     - DOIT être: `9A:4B:00:51:E4:0B:AB:8B:D9:DB:53:42:DD:15:A6:84:48:F7:3D:94`
     - Format: `XX:XX:XX:...` avec des deux-points

6. **Si le Client ID n'existe pas ou est incorrect**:
   - Cliquez sur **Create Credentials** > **OAuth client ID**
   - Sélectionnez **Android** comme type d'application
   - Entrez:
     - Name: `WayFinder Android` (ou autre nom)
     - Package name: `tn.esprit.WayFinder`
     - SHA-1 certificate fingerprint: `9A:4B:00:51:E4:0B:AB:8B:D9:DB:53:42:DD:15:A6:84:48:F7:3D:94`
   - Cliquez sur **Create**
   - **Copiez le Client ID** généré

7. **Vérifiez que le Client ID correspond**:
   - Le Client ID dans Google Cloud Console doit être: `825004975196-bovfg53oqaurlfmhehoeva4on629uc1e.apps.googleusercontent.com`
   - Si ce n'est pas le même, mettez à jour `strings.xml` avec le bon Client ID

### 3. Vérification du backend (Render.com)

Assurez-vous que la variable d'environnement suivante est configurée sur Render.com:

- **Key**: `GOOGLE_CLIENT_ID_ANDROID`
- **Value**: `825004975196-bovfg53oqaurlfmhehoeva4on629uc1e.apps.googleusercontent.com`

### 4. Après avoir modifié la configuration

1. **Sauvegardez** les modifications dans Google Cloud Console
2. **Attendez 2-5 minutes** pour que les changements se propagent
3. **Désinstallez complètement l'app** de l'émulateur/téléphone:
   - Long press sur l'icône de l'app > Désinstaller
   - Ou: `adb uninstall tn.esprit.WayFinder`
4. **Rebuild l'application** dans Android Studio:
   - Build > Clean Project
   - Build > Rebuild Project
5. **Réinstallez et testez** à nouveau

### 5. Erreurs communes

#### "Package name mismatch"
- Vérifiez que le package name dans Google Cloud Console est EXACTEMENT `tn.esprit.WayFinder`
- Vérifiez que `applicationId` dans `build.gradle.kts` est `tn.esprit.WayFinder`

#### "SHA-1 certificate fingerprint mismatch"
- Vérifiez que le SHA-1 dans Google Cloud Console est `9A:4B:00:51:E4:0B:AB:8B:D9:DB:53:42:DD:15:A6:84:48:F7:3D:94`
- Si vous utilisez un release keystore, ajoutez aussi le SHA-1 du release keystore

#### "Client ID not found"
- Vérifiez que le Client ID dans `strings.xml` correspond exactement au Client ID dans Google Cloud Console
- Le Client ID doit se terminer par `.apps.googleusercontent.com`

## Test de vérification

Pour vérifier que tout est correctement configuré:

1. Cliquez sur le bouton "Google" dans l'app
2. Si l'écran Google Sign-In s'ouvre et vous permet de sélectionner un compte → Configuration OAuth correcte ✅
3. Si vous pouvez sélectionner un compte mais que ça annule → Vérifiez le SHA-1 et package name
4. Si l'écran ne s'ouvre pas du tout → Vérifiez le Client ID dans `strings.xml`

## Support

Si le problème persiste après avoir vérifié toutes ces étapes:
1. Vérifiez les logs Logcat pour les erreurs spécifiques
2. Vérifiez que vous utilisez bien le debug keystore (pour le développement)
3. Vérifiez que l'API Google Sign-In est activée dans Google Cloud Console

