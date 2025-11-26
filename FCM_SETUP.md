# Configuration FCM (Firebase Cloud Messaging) pour iOS

Ce document explique comment configurer les notifications push FCM dans l'application iOS WayFinder.

## Prérequis

1. Un projet Firebase configuré avec FCM activé
2. Un fichier `GoogleService-Info.plist` téléchargé depuis la console Firebase
3. Les dépendances Firebase installées via Swift Package Manager ou CocoaPods

## Étapes de configuration

### 1. Ajouter le fichier GoogleService-Info.plist

1. Téléchargez le fichier `GoogleService-Info.plist` depuis la console Firebase :
   - Allez dans Firebase Console > Project Settings > General
   - Téléchargez le fichier `GoogleService-Info.plist` pour iOS
   
2. Ajoutez le fichier au projet Xcode :
   - Glissez-déposez le fichier dans le dossier `WayFinder` du projet
   - Cochez "Copy items if needed"
   - Assurez-vous que le fichier est ajouté à la target "WayFinder"

### 2. Installer les dépendances Firebase

#### Option A : Swift Package Manager (Recommandé)

1. Dans Xcode, allez dans File > Add Packages...
2. Ajoutez les packages suivants :
   - `https://github.com/firebase/firebase-ios-sdk`
3. Sélectionnez les produits suivants :
   - `FirebaseCore`
   - `FirebaseMessaging`

#### Option B : CocoaPods

Si vous utilisez CocoaPods, ajoutez à votre `Podfile` :

```ruby
pod 'Firebase/Core'
pod 'Firebase/Messaging'
```

Puis exécutez `pod install`.

### 3. Configurer les capacités Push Notifications

1. Dans Xcode, sélectionnez le projet "WayFinder"
2. Allez dans l'onglet "Signing & Capabilities"
3. Cliquez sur "+ Capability"
4. Ajoutez "Push Notifications"
5. Ajoutez également "Background Modes" et cochez "Remote notifications"

### 4. Configurer les certificats APNs

1. Dans Firebase Console, allez dans Project Settings > Cloud Messaging
2. Téléchargez votre certificat APNs ou configurez votre clé APNs
3. Assurez-vous que les certificats sont à jour

### 5. Variables d'environnement dans Render

Assurez-vous que les variables d'environnement suivantes sont configurées dans Render :

- `FIREBASE_SERVICE_ACCOUNT_KEY` : La clé du compte de service Firebase (JSON)
- OU `FIREBASE_SERVICE_ACCOUNT_PATH` : Le chemin vers le fichier de clé de compte de service

Ces variables sont nécessaires pour que le backend puisse envoyer des notifications FCM.

## Fonctionnalités implémentées

### Types de notifications supportés

L'application iOS supporte maintenant les notifications suivantes :

1. **Réservations** :
   - `booking_confirmed` : Réservation confirmée
   - `booking_cancelled` : Réservation annulée
   - `booking_updated` : Réservation mise à jour

2. **Paiements** :
   - `payment_success` : Paiement réussi
   - `payment_failed` : Échec du paiement

3. **Alertes** :
   - `price_alert` : Alerte de prix
   - `trip_reminder` : Rappel de voyage

4. **Social** :
   - `post_liked` : Un utilisateur a aimé votre post
   - `post_commented` : Un utilisateur a commenté votre post
   - `journey_liked` : Un utilisateur a aimé votre voyage partagé
   - `journey_commented` : Un utilisateur a commenté votre voyage partagé

### Comportement des notifications

- **En foreground** : Les notifications s'affichent même quand l'application est ouverte
- **En background** : Les notifications s'affichent dans le centre de notifications
- **Au tap** : Les notifications peuvent naviguer vers l'écran approprié via `actionUrl`

### Enregistrement du token FCM

Le token FCM est automatiquement :
- Récupéré au démarrage de l'application
- Enregistré auprès du backend après chaque connexion
- Mis à jour automatiquement si le token change

## Test des notifications

Pour tester les notifications :

1. Connectez-vous à l'application
2. Assurez-vous que les permissions de notifications sont accordées
3. Le token FCM sera automatiquement enregistré
4. Le backend enverra des notifications lors des événements suivants :
   - Quand quelqu'un aime ou commente votre post de discussion
   - Quand quelqu'un aime ou commente votre voyage partagé
   - Quand une réservation est confirmée

## Dépannage

### Les notifications ne s'affichent pas

1. Vérifiez que les permissions de notifications sont accordées dans les réglages iOS
2. Vérifiez que le fichier `GoogleService-Info.plist` est correctement ajouté au projet
3. Vérifiez les logs dans la console Xcode pour voir les erreurs FCM
4. Vérifiez que les certificats APNs sont correctement configurés dans Firebase

### Le token FCM n'est pas enregistré

1. Vérifiez que l'utilisateur est connecté (le token n'est enregistré qu'après la connexion)
2. Vérifiez les logs dans la console pour voir les erreurs d'enregistrement
3. Vérifiez que l'endpoint `/user/fcm-token` fonctionne correctement dans le backend

## Architecture

### Fichiers créés/modifiés

1. **FirebaseMessagingService.swift** : Service principal pour gérer FCM
   - Gère l'initialisation de Firebase
   - Gère les tokens FCM
   - Gère la réception des notifications
   - Gère l'affichage des notifications

2. **WayFinderAppDelegate.swift** : AppDelegate pour gérer les notifications push
   - Initialise Firebase au démarrage
   - Gère l'enregistrement pour les notifications distantes

3. **WayFinderApp.swift** : Point d'entrée de l'application
   - Configure l'AppDelegate
   - Initialise le service FCM

4. **AuthService.swift** : Service d'authentification
   - Enregistre le token FCM après chaque connexion

5. **UserService.swift** : Service utilisateur
   - Méthode `registerFcmToken` pour envoyer le token au backend

6. **NotificationModels.swift** : Modèles de données
   - Ajout des nouveaux types de notifications (post_liked, post_commented, journey_liked, journey_commented)

7. **NotificationView.swift** : Vue des notifications
   - Ajout des icônes pour les nouveaux types de notifications

## Notes importantes

- Le token FCM n'est enregistré qu'après la connexion de l'utilisateur
- Les notifications nécessitent les permissions utilisateur
- Les notifications fonctionnent en foreground et en background
- Le backend doit avoir Firebase Admin SDK configuré pour envoyer les notifications

