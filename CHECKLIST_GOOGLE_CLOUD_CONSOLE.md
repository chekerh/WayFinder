# Checklist Complète - Google Cloud Console Configuration

## ⚠️ IMPORTANT : L'erreur DEVELOPER_ERROR persiste

Cette erreur signifie que **la configuration OAuth dans Google Cloud Console ne correspond PAS à votre application**.

## ✅ Checklist de Vérification (À faire DANS L'ORDRE)

### ÉTAPE 1 : Activer l'API Google Sign-In (CRITIQUE)

1. Allez sur **https://console.cloud.google.com/**
2. Sélectionnez **votre projet** (celui avec Client ID `825004975196-...`)
3. Allez dans **APIs & Services** > **Library**
4. Recherchez **"Google Sign-In API"** ou **"Identity Toolkit API"**
5. **Si elle n'est PAS activée**, cliquez sur **Enable**
6. ⏰ **Attendez 2-3 minutes**

**Cette étape est CRITIQUE. Si l'API n'est pas activée, DEVELOPER_ERROR se produira toujours.**

---

### ÉTAPE 2 : Configurer OAuth Consent Screen (OBLIGATOIRE)

1. Dans Google Cloud Console, allez dans **APIs & Services** > **OAuth consent screen**
2. Si c'est la première fois :
   - **User Type**: Sélectionnez **External** (ou Internal si vous avez un compte G Suite)
   - Cliquez sur **Create**
   - **App name**: `WayFinder` (ou autre nom)
   - **User support email**: Votre email
   - **Developer contact information**: Votre email
   - Cliquez sur **Save and Continue**
   - **Scopes**: Cliquez sur **Save and Continue** (les scopes par défaut suffisent)
   - **Test users**: Si vous êtes en mode Testing, ajoutez votre email Google
   - Cliquez sur **Save and Continue**
   - Cliquez sur **Back to Dashboard**

**Si le OAuth consent screen n'est pas configuré, DEVELOPER_ERROR peut se produire.**

---

### ÉTAPE 3 : Vérifier/Créer le OAuth Client ID Android

1. Allez dans **APIs & Services** > **Credentials**
2. Recherchez votre OAuth Client ID avec le Client ID: `825004975196-gr4rffsv0akatmmn9qonoqsq5aa4q5e6`
3. **Ouvrez-le** (cliquez dessus)

#### Vérifiez EXACTEMENT ces valeurs (COPIEZ-COLLEZ) :

```
Application type: Android

Name: WayFinder Android (ou autre nom - n'importe lequel)

Package name: tn.esprit.WayFinder
⚠️ IMPORTANT: Copiez-collez EXACTEMENT:
- W majuscule
- F majuscule  
- Pas d'espaces avant ou après
- Pas de caractères cachés

SHA-1 certificate fingerprint: 9A:4B:00:51:E4:0B:AB:8B:D9:DB:53:42:DD:15:A6:84:48:F7:3D:94
⚠️ IMPORTANT: Copiez-collez EXACTEMENT:
- Format avec deux-points (:)
- Toutes en majuscules
- Pas d'espaces
- Pas de retours à la ligne
```

4. **Si ces valeurs sont DIFFÉRENTES**, modifiez-les et cliquez sur **Save**
5. ⏰ **Attendez 5-10 minutes** pour la propagation

---

### ÉTAPE 4 : Vérifier que le Client ID dans strings.xml correspond

1. Ouvrez `android/app/src/main/res/values/strings.xml`
2. Vérifiez que le Client ID est : `825004975196-gr4rffsv0akatmmn9qonoqsq5aa4q5e6.apps.googleusercontent.com`
3. **Sauvegardez** (Cmd+S)

---

### ÉTAPE 5 : Vérifier le package name dans build.gradle.kts

1. Ouvrez `android/app/build.gradle.kts`
2. Vérifiez ligne 13 :
   ```kotlin
   applicationId = "tn.esprit.WayFinder"
   ```
3. **Doit être EXACTEMENT** `tn.esprit.WayFinder` (W et F majuscules)

---

### ÉTAPE 6 : Désinstaller complètement l'application

**TRÈS IMPORTANT**: L'app peut avoir mis en cache l'ancienne configuration.

1. **Arrêtez l'application** dans l'émulateur
2. **Désinstallez complètement** :
   - Long press sur l'icône WayFinder > **Désinstaller**
   - Ou : Settings > Apps > WayFinder > **Uninstall**
3. **Redémarrez l'émulateur** (optionnel mais recommandé)

---

### ÉTAPE 7 : Clean et Rebuild dans Android Studio

1. **Build** > **Clean Project**
2. Attendez que le clean se termine
3. **Build** > **Rebuild Project**
4. Attendez que le rebuild se termine (vérifiez qu'il n'y a pas d'erreurs)

---

### ÉTAPE 8 : Attendre et Réinstaller

1. ⏰ **Attendez encore 5-10 minutes** après toutes les modifications dans Google Cloud Console
2. **Run** l'application dans Android Studio
3. **Testez** le bouton Google

---

## 🐛 Si ça ne fonctionne TOUJOURS pas

### Option A : Vérifier que vous utilisez le bon projet

Le Client ID `825004975196-...` doit être dans le **même projet** que celui actuellement sélectionné dans Google Cloud Console.

### Option B : Créer un nouveau projet Google Cloud

1. Créez un **nouveau projet** dans Google Cloud Console
2. Activez **Google Sign-In API**
3. Configurez **OAuth consent screen**
4. Créez un **nouveau OAuth Client ID Android** avec les valeurs exactes
5. Mettez à jour `strings.xml` avec le nouveau Client ID
6. Attendez 15 minutes
7. Désinstallez l'app et rebuild

### Option C : Vérifier le SHA-1 à nouveau

Obtenez le SHA-1 à nouveau pour confirmer :

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
```

Vérifiez que c'est bien : `9A:4B:00:51:E4:0B:AB:8B:D9:DB:53:42:DD:15:A6:84:48:F7:3D:94`

---

## 📋 Résumé des valeurs à copier-coller

```
Package name:
tn.esprit.WayFinder

SHA-1:
9A:4B:00:51:E4:0B:AB:8B:D9:DB:53:42:DD:15:A6:84:48:F7:3D:94
```

**COPIEZ-COLLEZ exactement ces valeurs dans Google Cloud Console !**

