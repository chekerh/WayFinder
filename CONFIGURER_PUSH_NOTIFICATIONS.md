# 🔔 Configurer Push Notifications dans Xcode

## ⚠️ Erreur actuelle

Vous avez cette erreur :
```
aucune autorisation « aps-environment » valide détectée pour l'application
```

Cela signifie que les **Push Notifications** ne sont pas configurées dans les Capabilities du projet Xcode.

---

## 📝 Solution : Ajouter Push Notifications Capability

### Étape 1 : Ouvrir le projet dans Xcode
1. Ouvrir `WayFinder.xcodeproj` dans Xcode

### Étape 2 : Aller dans Signing & Capabilities
1. Dans le navigateur de projet (gauche), sélectionner le projet **"WayFinder"** (icône bleue en haut)
2. Sélectionner la target **"WayFinder"** (sous "TARGETS")
3. Aller dans l'onglet **"Signing & Capabilities"** (en haut)

### Étape 3 : Ajouter Push Notifications
1. Cliquer sur le bouton **"+ Capability"** (en haut à gauche)
2. Chercher **"Push Notifications"**
3. Double-cliquer ou cliquer sur **"Push Notifications"** pour l'ajouter

### Étape 4 : Ajouter Background Modes (optionnel mais recommandé)
1. Toujours dans **"Signing & Capabilities"**
2. Cliquer à nouveau sur **"+ Capability"**
3. Chercher **"Background Modes"**
4. Double-cliquer pour l'ajouter
5. Cocher **"Remote notifications"** dans la liste

---

## ✅ Vérification

Après avoir ajouté les capabilities, vous devriez voir :
- ✅ **Push Notifications** dans la liste des Capabilities
- ✅ **Background Modes** avec "Remote notifications" coché

---

## 🔄 Recompiler

1. **Product** > **Clean Build Folder** (Shift+Cmd+K)
2. **Product** > **Build** (Cmd+B)
3. Lancer sur un **appareil réel** (pas le simulateur - les notifications push ne fonctionnent pas sur simulateur)

---

## 📱 Tester sur appareil réel

⚠️ **IMPORTANT** : Les notifications push ne fonctionnent **PAS** sur le simulateur iOS. Vous devez tester sur un **vrai iPhone/iPad**.

1. Connecter votre iPhone/iPad à votre Mac
2. Dans Xcode, sélectionner votre appareil dans la liste des devices
3. Lancer l'app (Cmd+R)
4. Autoriser les notifications quand demandé
5. Vérifier dans les logs Xcode que le token FCM est bien reçu

---

## 🎯 Résultat attendu

Après configuration, vous devriez voir dans les logs :
- ✅ `✅ [FCM] APNs device token received`
- ✅ `✅ [FCM] FCM Token received: ...`
- ✅ Plus d'erreur "No APNS token specified"
- ✅ Plus d'erreur "aps-environment"

---

## 🆘 Si ça ne fonctionne toujours pas

1. Vérifier que vous testez sur un **appareil réel** (pas simulateur)
2. Vérifier que vous avez un compte développeur Apple configuré
3. Vérifier que le **Bundle Identifier** correspond à votre compte développeur
4. Vérifier que **GoogleService-Info.plist** est bien dans le projet

