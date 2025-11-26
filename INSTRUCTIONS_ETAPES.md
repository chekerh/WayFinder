# Instructions - Étapes à faire manuellement

## ✅ Ce que vous DEVEZ faire vous-même

### 1. Installer CocoaPods (si pas déjà installé)
```bash
sudo gem install cocoapods
```

### 2. Installer les dépendances Firebase
```bash
cd /Users/sarrachmek/Desktop/dam/ios
pod install
```

### 3. Ajouter le fichier GoogleService-Info.plist
- Téléchargez depuis Firebase Console > Project Settings > General
- Glissez-le dans le dossier `WayFinder` dans Xcode
- Cochez "Copy items if needed"

### 4. Configurer les capacités dans Xcode
- Ouvrez `WayFinder.xcworkspace` (pas .xcodeproj)
- Sélectionnez le projet "WayFinder"
- Onglet "Signing & Capabilities"
- Cliquez "+ Capability"
- Ajoutez "Push Notifications"
- Ajoutez "Background Modes" et cochez "Remote notifications"

### 5. Configurer les certificats APNs dans Firebase
- Firebase Console > Project Settings > Cloud Messaging
- Configurez votre certificat APNs ou clé APNs

## ❌ Ce que vous N'AVEZ PAS besoin de faire

- ✅ Le code est déjà écrit et fonctionnel
- ✅ Le backend est déjà configuré (votre collègue l'a fait)
- ✅ Les endpoints API sont déjà en place
- ✅ Le service FCM backend est déjà implémenté

## 🔄 Différence Android vs iOS

### Android (ce que votre collègue a fait)
- Utilise **Gradle** (système de build Android) pour les dépendances
- Les dépendances sont dans `build.gradle.kts`
- Pas besoin de packages séparés, tout est géré par Gradle
- C'est pourquoi votre amie dit "non" pour les packages - Android utilise Gradle, pas des packages comme iOS

### iOS (ce que vous devez faire)
- Utilise **CocoaPods** ou **Swift Package Manager** pour les dépendances
- Les dépendances doivent être installées manuellement
- C'est différent d'Android, donc vous devez installer les pods

## 📋 Checklist rapide

- [ ] CocoaPods installé (`pod --version`)
- [ ] `pod install` exécuté
- [ ] `WayFinder.xcworkspace` ouvert (pas .xcodeproj)
- [ ] `GoogleService-Info.plist` ajouté au projet
- [ ] Push Notifications capability ajoutée
- [ ] Background Modes configuré
- [ ] Certificats APNs configurés dans Firebase

Une fois ces étapes faites, tout devrait fonctionner ! 🎉

