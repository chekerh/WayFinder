# 🚀 Guide Rapide : Migration vers Swift Package Manager (SPM)

## Pourquoi SPM au lieu de CocoaPods ?

**Swift Package Manager (SPM)** est l'équivalent de **Gradle sur Android**. C'est le gestionnaire de dépendances natif d'Apple, intégré directement dans Xcode. Plus besoin de CocoaPods ! 🎉

---

## 📝 Étapes simples (5 minutes)

### 1️⃣ Ouvrir le projet dans Xcode
- Ouvrir **`WayFinder.xcodeproj`** directement (plus besoin du `.xcworkspace` !)
- Si Xcode est déjà ouvert, le fermer complètement et rouvrir le `.xcodeproj`

### 2️⃣ Ajouter Firebase via SPM
1. Dans Xcode : **File** > **Add Package Dependencies...**
2. Coller cette URL dans la barre de recherche :
   ```
   https://github.com/firebase/firebase-ios-sdk
   ```
3. Cliquer sur **"Add Package"**
4. Sélectionner :
   - ✅ **FirebaseCore**
   - ✅ **FirebaseMessaging**
5. S'assurer que la target **"WayFinder"** est cochée
6. Cliquer sur **"Add Package"**

### 3️⃣ Vérifier que ça fonctionne
- **Product** > **Clean Build Folder** (Shift+Cmd+K)
- **Product** > **Build** (Cmd+B)
- Si ça compile sans erreur, c'est bon ! ✅

### 4️⃣ (Optionnel) Nettoyer CocoaPods
Une fois que vous avez vérifié que tout fonctionne, vous pouvez supprimer CocoaPods :

```bash
cd /Users/sarrachmek/Desktop/dam/ios
./cleanup_cocoapods.sh
```

Ou manuellement :
```bash
rm -rf Pods/ Podfile.lock WayFinder.xcworkspace
```

---

## ✅ C'est tout !

Maintenant vous pouvez :
- ✅ Ouvrir directement `WayFinder.xcodeproj` (pas besoin de `.xcworkspace`)
- ✅ Utiliser Firebase sans CocoaPods
- ✅ Gérer les dépendances directement dans Xcode (comme Gradle sur Android)

---

## 🔧 En cas de problème

**Erreur "No such module 'FirebaseCore'" ?**
1. Vérifier que le package est bien ajouté dans Xcode (section "Package Dependencies")
2. **File** > **Packages** > **Reset Package Caches**
3. **File** > **Packages** > **Update to Latest Package Versions**
4. Nettoyer le build : **Product** > **Clean Build Folder**

---

## 📚 Documentation complète

Pour plus de détails, voir : `MIGRATION_SPM_FIREBASE.md`

