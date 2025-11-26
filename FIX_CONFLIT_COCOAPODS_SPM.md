# 🔧 Résolution du conflit CocoaPods / Swift Package Manager

## 🚨 Problème identifié

Vous avez une erreur **"Redefinition of module 'Firebase'"** car :
- ✅ Firebase est installé via **Swift Package Manager** (SPM) 
- ❌ Mais les **références CocoaPods** sont encore présentes dans le projet

Il faut supprimer toutes les références CocoaPods pour que SPM fonctionne correctement.

---

## 📝 Solution étape par étape

### Étape 1 : Fermer Xcode complètement

⚠️ **IMPORTANT** : Fermez Xcode complètement (Cmd+Q) avant de continuer.

---

### Étape 2 : Supprimer les fichiers CocoaPods

Ouvrez le Terminal et exécutez :

```bash
cd /Users/sarrachmek/Desktop/dam/ios

# Supprimer les fichiers CocoaPods
rm -rf Pods/
rm -rf Podfile.lock
rm -rf WayFinder.xcworkspace
```

---

### Étape 3 : Ouvrir le projet dans Xcode

1. Ouvrir **`WayFinder.xcodeproj`** (pas le `.xcworkspace` qui n'existe plus)
2. Attendre que Xcode charge le projet

---

### Étape 4 : Supprimer les références Pods dans "Build Phases"

1. Dans Xcode, sélectionner le projet **"WayFinder"** (icône bleue en haut à gauche)
2. Sélectionner la target **"WayFinder"** (pas les tests)
3. Aller dans l'onglet **"Build Phases"**
4. Déplier la section **"Link Binary With Libraries"**
5. Chercher et **supprimer** (clic droit > Delete ou sélectionner et appuyer sur Delete) :
   - ❌ `Pods_WayFinder.framework` (si présent)
   - ❌ Toute autre référence aux Pods

⚠️ **GARDER** :
   - ✅ `FirebaseCore` (de SPM)
   - ✅ `FirebaseMessaging` (de SPM)
   - ✅ `GoogleSignIn` (si vous l'utilisez)

---

### Étape 5 : Supprimer les références .xcconfig dans "Build Settings"

1. Toujours dans la target **"WayFinder"**, aller dans l'onglet **"Build Settings"**
2. En haut à droite, cliquer sur **"All"** et **"Combined"** pour voir tous les paramètres
3. Chercher **"baseConfigurationReference"** ou **"Config File"**
4. Si vous voyez des références à des fichiers `.xcconfig` contenant "Pods", les supprimer

**OU** chercher dans la barre de recherche en haut :
- Chercher **"Pods"**
- Si vous trouvez des paramètres qui référencent des fichiers `.xcconfig` des Pods, les supprimer

---

### Étape 6 : Faire la même chose pour les targets de tests (optionnel)

Si vous avez des erreurs avec les tests :

1. Sélectionner la target **"WayFinderTests"**
2. Répéter les étapes 4 et 5
3. Faire de même pour **"WayFinderUITests"**

---

### Étape 7 : Nettoyer et reconstruire

1. Dans Xcode : **Product** > **Clean Build Folder** (Shift+Cmd+K)
2. **File** > **Packages** > **Reset Package Caches**
3. **File** > **Packages** > **Update to Latest Package Versions**
4. **Product** > **Build** (Cmd+B)

---

## ✅ Vérification

Si tout fonctionne, vous devriez :
- ✅ Voir **"Build Succeeded"** dans Xcode
- ✅ Plus d'erreur "Redefinition of module 'Firebase'"
- ✅ Plus d'erreur "Could not build Objective-C module 'FirebaseCore'"

---

## 🆘 Si ça ne fonctionne toujours pas

1. **Fermer Xcode complètement**
2. Supprimer le cache DerivedData :
   ```bash
   rm -rf ~/Library/Developer/Xcode/DerivedData/WayFinder-*
   ```
3. **Rouvrir** `WayFinder.xcodeproj`
4. **Nettoyer** et **reconstruire**

---

## 📚 Alternative : Réinitialiser complètement SPM

Si les erreurs persistent :

1. Dans Xcode : **File** > **Packages** > **Reset Package Caches**
2. **File** > **Packages** > **Update to Latest Package Versions**
3. Attendre que les packages se téléchargent
4. Nettoyer et reconstruire

---

## 🎉 Résultat attendu

Une fois terminé, vous devriez avoir :
- ✅ Firebase fonctionnant uniquement via **Swift Package Manager**
- ✅ Plus de conflit avec CocoaPods
- ✅ Projet qui compile sans erreurs

