# Configuration CocoaPods pour Firebase

Ce guide vous explique comment installer Firebase via CocoaPods pour résoudre l'erreur "No such module 'FirebaseCore'".

## Prérequis

1. CocoaPods installé sur votre Mac. Si ce n'est pas le cas, installez-le avec :
   ```bash
   sudo gem install cocoapods
   ```

## Étapes d'installation

### 1. Ouvrir le terminal et naviguer vers le dossier ios

```bash
cd /Users/sarrachmek/Desktop/dam/ios
```

### 2. Installer les pods

```bash
pod install
```

Cette commande va :
- Lire le fichier `Podfile`
- Télécharger et installer Firebase et ses dépendances
- Créer un fichier `WayFinder.xcworkspace`

### 3. Fermer Xcode si ouvert

⚠️ **IMPORTANT** : Fermez complètement Xcode avant de continuer.

### 4. Ouvrir le workspace (pas le projet)

Au lieu d'ouvrir `WayFinder.xcodeproj`, vous devez maintenant ouvrir `WayFinder.xcworkspace` :

```bash
open WayFinder.xcworkspace
```

Ou depuis Finder :
- Naviguez vers `/Users/sarrachmek/Desktop/dam/ios/`
- Double-cliquez sur `WayFinder.xcworkspace` (pas sur `WayFinder.xcodeproj`)

### 5. Vérifier l'installation

Une fois le workspace ouvert dans Xcode :
1. Vérifiez que vous voyez un nouveau dossier "Pods" dans le navigateur de projet
2. Essayez de compiler le projet (Cmd+B)
3. L'erreur "No such module 'FirebaseCore'" devrait être résolue

## Structure après installation

Après `pod install`, vous devriez avoir :

```
ios/
├── Podfile                    # Fichier de configuration CocoaPods
├── Podfile.lock              # Verrouille les versions des pods
├── Pods/                      # Dossier contenant les dépendances
│   └── ...
├── WayFinder.xcworkspace     # ⚠️ Ouvrir ce fichier, pas le .xcodeproj
└── WayFinder.xcodeproj       # Ne plus ouvrir directement
```

## Commandes utiles

### Mettre à jour les pods
```bash
pod update
```

### Mettre à jour un pod spécifique
```bash
pod update Firebase/Messaging
```

### Voir les pods installés
```bash
pod list
```

### Nettoyer et réinstaller
```bash
pod deintegrate
pod install
```

## Dépannage

### Erreur "pod: command not found"
Installez CocoaPods :
```bash
sudo gem install cocoapods
```

### Erreur de permissions
Si vous avez des problèmes de permissions :
```bash
sudo gem install cocoapods
```

### Le workspace ne s'ouvre pas
Assurez-vous d'avoir exécuté `pod install` et que le fichier `WayFinder.xcworkspace` existe.

### Les pods ne sont pas visibles dans Xcode
1. Fermez Xcode complètement
2. Supprimez le dossier `DerivedData` :
   ```bash
   rm -rf ~/Library/Developer/Xcode/DerivedData
   ```
3. Rouvrez le workspace

### Erreur "Unable to find a specification"
Mettez à jour le repo CocoaPods :
```bash
pod repo update
pod install
```

## Notes importantes

- ⚠️ **Toujours ouvrir le `.xcworkspace`, jamais le `.xcodeproj`** après avoir installé CocoaPods
- Le fichier `Podfile.lock` doit être commité dans Git pour verrouiller les versions
- Le dossier `Pods/` ne doit généralement **pas** être commité (ajoutez-le au `.gitignore`)

## Prochaines étapes

Une fois CocoaPods configuré et Firebase installé :

1. Ajoutez le fichier `GoogleService-Info.plist` depuis Firebase Console
2. Configurez les capacités Push Notifications dans Xcode
3. Testez l'application

Voir `FCM_SETUP.md` pour les détails complets de la configuration FCM.

