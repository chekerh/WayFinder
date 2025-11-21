# Guide de test - Intégration Pays par Région

Ce guide t'aide à tester si l'intégration backend/frontend fonctionne correctement.

---

## 📋 Prérequis

1. **Backend NestJS accessible**
   - Déploiement Render (défaut iOS) : `https://wayfinder-api-w92x.onrender.com/api`
   - Local : `http://127.0.0.1:3000/api` (ou l'IP de ton Mac si tu testes sur un device physique)
2. **Endpoints backend fonctionnels** :
   - `GET /api/regions/europe/countries`
   - `GET /api/regions/asia/countries`
   - `GET /api/regions/australia/countries`
   - `GET /api/countries/{countryId}`

---

## 🧪 Méthode 1 : Test direct des endpoints (curl/Postman)

Avant de tester dans l'app, vérifie que ton backend répond bien.

### Test 1 : Liste des pays d'Europe
```bash
curl -X GET "https://wayfinder-api-w92x.onrender.com/api/regions/europe/countries" \
  -H "Content-Type: application/json"
```
> Pour un backend local, remplace la base d'URL par `http://127.0.0.1:3000/api`.

**Résultat attendu** : JSON avec un tableau de pays, chaque pays ayant :
- `id` (ex: "italy-rome")
- `name`
- `summary`
- `thumbnailImageUrl`
- `heroImageUrl`
- `regionId` (devrait être "europe")

### Test 2 : Liste des pays d'Asie
```bash
curl -X GET "https://wayfinder-api-w92x.onrender.com/api/regions/asia/countries" \
  -H "Content-Type: application/json"
```

### Test 3 : Liste des pays d'Australie
```bash
curl -X GET "https://wayfinder-api-w92x.onrender.com/api/regions/australia/countries" \
  -H "Content-Type: application/json"
```

### Test 4 : Détail d'un pays
Remplace `{countryId}` par un ID réel retourné par les tests précédents (ex: "italy-rome") :
```bash
curl -X GET "https://wayfinder-api-w92x.onrender.com/api/countries/italy-rome" \
  -H "Content-Type: application/json"
```

**Résultat attendu** : JSON avec :
- `id`, `name`, `summary`, `heroImageUrl`, `description`
- `bestSeason`, `currency`, `language`, `timezone`
- `extraImages` (tableau d'URLs)

---

## 📱 Méthode 2 : Test dans l'app iOS

### Étape 1 : Vérifier la configuration

1. Ouvre `WayFinder/Networking/APIConfig.swift`
2. Vérifie que `baseURL` pointe vers ton backend :
   ```swift
   static let baseURL = URL(string: "https://wayfinder-api-w92x.onrender.com/api")!
   ```
   - **Déploiement Render** : valeur par défaut ci-dessus.
   - **Local (simulateur)** : `http://127.0.0.1:3000/api`.
   - **Local (device)** : IP locale du Mac (ex: `http://192.168.0.42:3000/api`).

### Étape 2 : Lancer l'app et observer les logs

1. **Ouvre la console Xcode** (⌘⇧Y ou View > Debug Area > Activate Console)
2. **Lance l'app** (⌘R)
3. **Connecte-toi** si nécessaire (ou passe l'authentification)
4. **Va sur l'écran Home** (HomeScreen)

### Étape 3 : Tester le clic sur une région

1. **Clique sur le chip "Europe"** (ou "Asie" / "Australie")
2. **Observe la console Xcode** - tu devrais voir :
   ```
   🔄 [CountryList] Loading countries for region: europe
   🌐 [API] GET https://wayfinder-api-w92x.onrender.com/api/regions/europe/countries
   📥 [API] Status: 200
   📥 [API] Response: [{"id":"italy-rome","name":"Rome, Italie",...}]
   ✅ [API] Decode success
   ✅ [CountryList] Loaded 5 countries
   ```

3. **Vérifie visuellement** :
   - L'écran de liste des pays s'affiche
   - Les pays sont listés avec leurs **thumbnails** (images `thumbnailImageUrl`)
   - Chaque pays affiche son **nom** et son **résumé** (`summary`)

### Étape 4 : Tester le clic sur un pays

1. **Clique sur une card de pays** dans la liste
2. **Observe la console** :
   ```
   🔄 [CountryDetail] Loading detail for country: italy-rome
   🌐 [API] GET https://wayfinder-api-w92x.onrender.com/api/countries/italy-rome
   📥 [API] Status: 200
   📥 [API] Response: {"id":"italy-rome","name":"Rome, Italie",...}
   ✅ [API] Decode success
   ✅ [CountryDetail] Loaded detail for: Rome, Italie
   ```

3. **Vérifie visuellement** :
   - La **grande image** (`heroImageUrl`) s'affiche en haut
   - Le **nom** du pays
   - Le **résumé** (`summary`)
   - La **description** complète
   - Les **infos supplémentaires** : meilleure période, monnaie, langue, fuseau horaire
   - Les **images supplémentaires** (`extraImages`) en carrousel horizontal

---

## 🔍 Méthode 3 : Vérification des erreurs

### Si tu vois des erreurs dans la console :

#### ❌ Erreur : "URL invalide"
- **Cause** : Problème de configuration `APIConfig.baseURL`
- **Solution** : Vérifie que l'URL est correcte et que le backend écoute bien

#### ❌ Erreur : "HTTP Error (code: 404)"
- **Cause** : L'endpoint n'existe pas ou le chemin est incorrect
- **Solution** : Vérifie que ton backend expose bien `/api/regions/{regionId}/countries` et `/api/countries/{countryId}`

#### ❌ Erreur : "HTTP Error (code: 401)"
- **Cause** : Authentification requise
- **Solution** : Vérifie si tes endpoints nécessitent un token JWT. Si oui, connecte-toi d'abord dans l'app.

#### ❌ Erreur : "Decode error"
- **Cause** : Le JSON renvoyé par le backend ne correspond pas aux modèles Swift
- **Solution** : Compare le JSON renvoyé (visible dans les logs `📥 [API] Response:`) avec les modèles dans `CountryModels.swift`

#### ❌ Erreur : "Erreur de connexion" / Timeout
- **Cause** : Le backend n'est pas accessible
- **Solution** :
  1. Vérifie que le backend est démarré (`npm run start:dev`)
  2. Vérifie l'URL dans `APIConfig.swift`
  3. Si tu testes sur un device physique, utilise l'IP locale du Mac (pas `localhost`)

---

## ✅ Checklist de validation

Coche chaque point quand c'est validé :

- [ ] Backend répond aux endpoints curl/Postman
- [ ] Les chips Europe/Asie/Australie s'affichent sur HomeScreen
- [ ] Clic sur un chip → navigation vers CountryListView
- [ ] La liste des pays se charge (logs visibles dans la console)
- [ ] Les pays s'affichent avec leurs thumbnails
- [ ] Clic sur un pays → navigation vers CountryDetailView
- [ ] Le détail se charge (logs visibles)
- [ ] La grande image (`heroImageUrl`) s'affiche
- [ ] Toutes les infos sont présentes (description, bestSeason, currency, etc.)
- [ ] Les images supplémentaires (`extraImages`) s'affichent en carrousel

---

## 🐛 Debug avancé

### Voir les requêtes réseau dans Xcode

1. Ouvre **Xcode** → **Product** → **Scheme** → **Edit Scheme**
2. Va dans **Run** → **Arguments**
3. Ajoute l'argument : `-NSURLSessionLoggingEnabled YES`
4. Relance l'app - tu verras les requêtes HTTP détaillées dans la console

### Tester avec Charles Proxy / Proxyman

Pour inspecter les requêtes HTTP en détail :
1. Installe [Charles Proxy](https://www.charlesproxy.com/) ou [Proxyman](https://proxyman.io/)
2. Configure le proxy sur ton Mac
3. Configure l'app iOS pour utiliser le proxy (Settings → Wi-Fi → HTTP Proxy)
4. Tu verras toutes les requêtes/réponses en temps réel

---

## 📝 Notes importantes

- Les logs avec emojis (🌐, 📥, ✅, ❌) sont visibles dans la console Xcode
- Si tu ne vois pas les logs, vérifie que le filtre de la console n'est pas activé
- Pour tester sur un device physique, assure-toi que le Mac et l'iPhone sont sur le même réseau Wi-Fi
- Les images (`thumbnailImageUrl`, `heroImageUrl`) doivent être des URLs accessibles (http/https valides)

---

Bon test ! 🚀

