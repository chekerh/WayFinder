# Configuration Google Sign-In pour WayFinder

## Étapes de configuration

### 1. Obtenir le Client ID Android depuis Google Cloud Console

1. Allez sur [Google Cloud Console](https://console.cloud.google.com/)
2. Sélectionnez votre projet (ou créez-en un nouveau)
3. Activez l'API "Google Sign-In" si ce n'est pas déjà fait
4. Allez dans **APIs & Services** > **Credentials**
5. Cliquez sur **Create Credentials** > **OAuth client ID**
6. Sélectionnez **Android** comme type d'application
7. Remplissez les informations :
   - **Name**: WayFinder Android (ou un nom de votre choix)
   - **Package name**: `tn.esprit.WayFinder` (doit correspondre exactement au package de l'app)
   - **SHA-1 certificate fingerprint**: Votre empreinte SHA-1 (voir ci-dessous)

### 2. Obtenir l'empreinte SHA-1

#### Pour le debug keystore (développement) :
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

#### Pour le release keystore (production) :
```bash
keytool -list -v -keystore /path/to/your/release.keystore -alias your_alias
```

Copiez la valeur **SHA1** (format: `XX:XX:XX:...`)

### 3. Configurer le Client ID dans l'application Android

1. Ouvrez le fichier `android/app/src/main/res/values/strings.xml`
2. Remplacez `YOUR_GOOGLE_CLIENT_ID_ANDROID_HERE` par votre Client ID Android :
   ```xml
   <string name="google_client_id_android">xxxxxx-xxxxx.apps.googleusercontent.com</string>
   ```

### 4. Configurer le Client ID dans le backend (Render.com)

1. Allez sur votre dashboard Render.com
2. Sélectionnez votre service backend
3. Allez dans **Environment**
4. Ajoutez la variable d'environnement :
   - **Key**: `GOOGLE_CLIENT_ID_ANDROID`
   - **Value**: Votre Client ID Android (le même que celui utilisé dans l'app Android)

### 5. Variables d'environnement requises sur Render.com

```
GOOGLE_CLIENT_ID_ANDROID=xxxxxx-xxxxx.apps.googleusercontent.com
```

**Note**: `GOOGLE_CLIENT_ID_WEB` et `GOOGLE_CLIENT_SECRET_WEB` sont **optionnels** et ne sont nécessaires que si vous avez un frontend web.

### 6. Configuration email (optionnel mais recommandé)

Pour la vérification d'email, configurez également :

```
EMAIL_SERVICE_HOST=smtp.gmail.com
EMAIL_SERVICE_PORT=587
EMAIL_SERVICE_USER=votre_email@gmail.com
EMAIL_SERVICE_PASS=votre_mot_de_passe_app
EMAIL_FROM_ADDRESS="WayFinder App <noreply@wayfinder.com>"
FRONTEND_URL=https://votre-app.com
```

## Test

1. Compilez et lancez l'application Android
2. Sur l'écran de connexion, cliquez sur le bouton "Google"
3. Sélectionnez votre compte Google
4. L'application devrait vous connecter automatiquement

## Dépannage

### Erreur: "Google Sign-In non configuré"
- Vérifiez que vous avez remplacé `YOUR_GOOGLE_CLIENT_ID_ANDROID_HERE` dans `strings.xml`

### Erreur: "Invalid token" ou "Unauthorized"
- Vérifiez que le Client ID dans l'app Android correspond exactement à celui dans Render.com
- Vérifiez que le package name dans Google Cloud Console correspond à `tn.esprit.WayFinder`
- Vérifiez que l'empreinte SHA-1 est correcte dans Google Cloud Console

### L'application ne se connecte pas après Google Sign-In
- Vérifiez les logs du backend sur Render.com
- Vérifiez que `GOOGLE_CLIENT_ID_ANDROID` est bien configuré dans Render.com

