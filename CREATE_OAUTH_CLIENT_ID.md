# Guide pour créer/corriger le OAuth Client ID dans Google Cloud Console

## Informations exactes de votre application

- **Package name**: `tn.esprit.WayFinder` (exactement, avec W et F majuscules)
- **SHA-1 (debug)**: `9A:4B:00:51:E4:0B:AB:8B:D9:DB:53:42:DD:15:A6:84:48:F7:3D:94`
- **Client ID actuel**: `825004975196-bovfg53oqaurlfmhehoeva4on629uc1e.apps.googleusercontent.com`

## Option 1 : Vérifier et corriger le Client ID existant

1. **Allez sur** https://console.cloud.google.com/
2. **Sélectionnez votre projet** (celui qui contient le Client ID `825004975196-...`)
3. **Allez dans** APIs & Services > **Credentials**
4. **Trouvez** votre OAuth 2.0 Client ID (type: **Android**)
5. **Cliquez dessus pour l'éditer**
6. **Vérifiez/modifiez** ces valeurs EXACTES :

   ```
   Name: WayFinder Android (ou n'importe quel nom)
   
   Package name: tn.esprit.WayFinder
   ⚠️ ATTENTION: 
   - W majuscule
   - F majuscule
   - Pas d'espaces
   - Pas de caractères supplémentaires
   
   SHA-1 certificate fingerprint: 9A:4B:00:51:E4:0B:AB:8B:D9:DB:53:42:DD:15:A6:84:48:F7:3D:94
   ⚠️ ATTENTION:
   - Format avec deux-points (:)
   - Majuscules
   - Pas d'espaces
   ```

7. **Cliquez sur** Save
8. **Attendez 5-10 minutes** (important !)

## Option 2 : Créer un nouveau OAuth Client ID

Si vous n'êtes pas sûr du Client ID existant ou s'il y a des problèmes :

1. **Allez sur** https://console.cloud.google.com/
2. **Sélectionnez votre projet**
3. **Allez dans** APIs & Services > **Credentials**
4. **Cliquez sur** + CREATE CREDENTIALS > **OAuth client ID**
5. **Si c'est la première fois**, configurez l'OAuth consent screen :
   - User Type: External (ou Internal selon votre compte)
   - Remplissez les informations de base (App name, User support email)
   - Cliquez sur Save and Continue
   - Scopes: Cliquez sur Save and Continue (les scopes par défaut suffisent)
   - Test users: Ajoutez votre email Google si nécessaire
   - Cliquez sur Back to Dashboard
6. **Créez le Client ID**:
   - Application type: **Android**
   - Name: `WayFinder Android` (ou autre nom)
   - Package name: `tn.esprit.WayFinder` (EXACTEMENT comme indiqué)
   - SHA-1 certificate fingerprint: `9A:4B:00:51:E4:0B:AB:8B:D9:DB:53:42:DD:15:A6:84:48:F7:3D:94`
   - Cliquez sur **Create**
7. **Copiez le Client ID** généré (format: `xxxxx-xxxxx.apps.googleusercontent.com`)
8. **Mettez à jour** `android/app/src/main/res/values/strings.xml`:
   ```xml
   <string name="google_client_id_android">VOTRE_NOUVEAU_CLIENT_ID_ICI</string>
   ```
9. **Attendez 5-10 minutes** avant de tester

## Vérifications après modification

### Vérifier que l'app est complètement désinstallée

1. **Dans Android Studio**, arrêtez l'émulateur
2. **Désinstallez manuellement l'app**:
   - Long press sur l'icône WayFinder > Désinstaller
   - Ou depuis le terminal: `adb uninstall tn.esprit.WayFinder`
3. **Redémarrez l'émulateur**
4. **Rebuild l'app**:
   - Build > Clean Project
   - Build > Rebuild Project
5. **Réinstallez et testez**

### Vérifier que la configuration est correcte

Après avoir modifié Google Cloud Console :

1. **Vérifiez visuellement** que le package name est EXACTEMENT `tn.esprit.WayFinder`
   - Pas `tn.esprit.wayfinder` (minuscules)
   - Pas `tn.esprit.Wayfinder` (un seul majuscule)
   - Pas d'espaces avant ou après
   
2. **Vérifiez que le SHA-1 est EXACTEMENT**:
   ```
   9A:4B:00:51:E4:0B:AB:8B:D9:DB:53:42:DD:15:A6:84:48:F7:3D:94
   ```
   - Avec des deux-points (:)
   - En majuscules
   - Pas d'espaces

3. **Attendez au moins 5-10 minutes** après avoir sauvegardé dans Google Cloud Console

4. **Si vous avez créé un nouveau Client ID**, mettez à jour `strings.xml` et rebuild

## Si ça ne fonctionne toujours pas

1. **Vérifiez les logs** pour confirmer que c'est toujours DEVELOPER_ERROR
2. **Vérifiez que vous utilisez le bon projet** dans Google Cloud Console
3. **Essayez de créer un nouveau Client ID** (Option 2) au lieu de modifier l'existant
4. **Vérifiez que l'API Google Sign-In est activée**:
   - APIs & Services > Library
   - Recherchez "Google Sign-In API" ou "Identity Toolkit API"
   - Assurez-vous qu'elle est activée

## Résumé des valeurs exactes à copier-coller

```
Package name:
tn.esprit.WayFinder

SHA-1:
9A:4B:00:51:E4:0B:AB:8B:D9:DB:53:42:DD:15:A6:84:48:F7:3D:94
```

**Copiez-collez exactement ces valeurs dans Google Cloud Console !**

