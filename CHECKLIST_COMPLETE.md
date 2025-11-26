# ✅ Checklist complète - Configuration FCM iOS

## Étape 1 : Installer CocoaPods ⏳
```bash
sudo gem install cocoapods
```
**Status** : En cours - Entrez votre mot de passe Mac

---

## Étape 2 : Installer les dépendances Firebase
Une fois CocoaPods installé, exécutez :
```bash
cd /Users/sarrachmek/Desktop/dam/ios
pod install
```
Cette commande va télécharger Firebase et créer le fichier `WayFinder.xcworkspace`

---

## Étape 3 : Fermer Xcode
⚠️ **IMPORTANT** : Fermez complètement Xcode si il est ouvert

---

## Étape 4 : Ouvrir le workspace (pas le projet)
Au lieu d'ouvrir `WayFinder.xcodeproj`, ouvrez maintenant :
- `WayFinder.xcworkspace` (le nouveau fichier créé par CocoaPods)

Depuis le terminal :
```bash
cd /Users/sarrachmek/Desktop/dam/ios
open WayFinder.xcworkspace
```

---

## Étape 5 : Ajouter GoogleService-Info.plist
1. Allez sur [Firebase Console](https://console.firebase.google.com/)
2. Sélectionnez votre projet
3. Allez dans **Project Settings** (⚙️) > **General**
4. Faites défiler jusqu'à "Your apps"
5. Cliquez sur l'icône iOS ou "Add app" si pas encore ajouté
6. Téléchargez le fichier `GoogleService-Info.plist`
7. Dans Xcode (avec le workspace ouvert) :
   - Glissez-déposez le fichier dans le dossier `WayFinder` (à côté de `WayFinderApp.swift`)
   - Cochez ✅ "Copy items if needed"
   - Cochez ✅ "WayFinder" dans "Add to targets"
   - Cliquez "Finish"

---

## Étape 6 : Configurer Push Notifications dans Xcode
1. Dans Xcode, sélectionnez le projet "WayFinder" (icône bleue en haut à gauche)
2. Sélectionnez la target "WayFinder" (pas "WayFinderTests")
3. Allez dans l'onglet **"Signing & Capabilities"**
4. Cliquez sur **"+ Capability"** (en haut à gauche)
5. Ajoutez **"Push Notifications"**
6. Cliquez à nouveau sur **"+ Capability"**
7. Ajoutez **"Background Modes"**
8. Dans Background Modes, cochez ✅ **"Remote notifications"**

---

## Étape 7 : Configurer les certificats APNs dans Firebase
1. Allez sur [Firebase Console](https://console.firebase.google.com/)
2. Sélectionnez votre projet
3. Allez dans **Project Settings** (⚙️) > **Cloud Messaging**
4. Dans la section "Apple app configuration" :
   - Si vous avez déjà un certificat APNs, téléchargez-le et uploadez-le
   - OU créez une nouvelle clé APNs :
     - Allez sur [Apple Developer](https://developer.apple.com/account/resources/authkeys/list)
     - Créez une nouvelle clé avec "Apple Push Notifications service (APNs)"
     - Téléchargez la clé (.p8)
     - Dans Firebase, uploadez la clé et entrez votre Team ID

---

## Étape 8 : Vérifier les variables d'environnement dans Render
Votre collègue a déjà ajouté les clés dans Render (comme sur votre capture d'écran), mais vérifiez que ces variables existent :
- `FIREBASE_SERVICE_ACCOUNT_KEY` (ou `FIREBASE_SERVICE_ACCOUNT_PATH`)

---

## Étape 9 : Tester
1. Compilez le projet dans Xcode (⌘ + B)
2. L'erreur "No such module 'FirebaseCore'" devrait disparaître
3. Lancez l'application sur un simulateur ou un appareil réel
4. Connectez-vous à l'application
5. Le token FCM sera automatiquement enregistré

---

## ✅ Résumé des fichiers à avoir

Après toutes les étapes, vous devriez avoir :
- ✅ `Podfile` (déjà créé)
- ✅ `Podfile.lock` (créé après `pod install`)
- ✅ Dossier `Pods/` (créé après `pod install`)
- ✅ `WayFinder.xcworkspace` (créé après `pod install`)
- ✅ `GoogleService-Info.plist` (à télécharger depuis Firebase)

---

## 🆘 Si vous avez des problèmes

### Erreur "pod: command not found"
→ CocoaPods n'est pas installé, refaites l'étape 1

### Erreur "No such module 'FirebaseCore'"
→ Vous avez ouvert `.xcodeproj` au lieu de `.xcworkspace`
→ Fermez Xcode et ouvrez `WayFinder.xcworkspace`

### Erreur lors de `pod install`
→ Vérifiez votre connexion internet
→ Essayez : `pod repo update` puis `pod install`

---

## 📝 Notes importantes

- ⚠️ **TOUJOURS ouvrir `.xcworkspace`, JAMAIS `.xcodeproj`** après avoir installé CocoaPods
- Le fichier `GoogleService-Info.plist` est unique à votre projet Firebase
- Les certificats APNs sont nécessaires pour que les notifications fonctionnent sur de vrais appareils
- Le simulateur iOS peut recevoir des notifications, mais c'est mieux de tester sur un vrai appareil

