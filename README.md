# WayFinder

WayFinder est une application iOS construite en SwiftUI qui personnalise l’expérience de voyage grâce à l’IA. Elle interroge l’utilisateur sur son style de voyage, son budget et ses préférences, puis recommande des vols, hôtels et activités adaptés. L’application s’appuie sur un backend NestJS pour l’authentification et la récupération de contenus dynamiques, tout en proposant une interface multilingue (FR / EN / AR), compatible mode clair / sombre et pensée pour l’accessibilité.

---

## Sommaire

- [Fonctionnalités principales](#fonctionnalités-principales)
- [Architecture & technique](#architecture--technique)
- [Prérequis](#prérequis)
- [Installation & lancement](#installation--lancement)
  - [Backend NestJS](#backend-nestjs)
  - [Application iOS](#application-ios)
- [Configuration](#configuration)
  - [Côté iOS](#côté-ios)
  - [Côté NestJS](#côté-nestjs)
- [Localisation et RTL](#localisation-et-rtl)
- [Mode sombre / clair](#mode-sombre--clair)
- [Ethique & protection des données](#ethique--protection-des-données)
- [Améliorations recommandées](#améliorations-recommandées)
- [Licence](#licence)

---

## Fonctionnalités principales

- **Personnalisation IA** : questionnaire léger (type de voyage, budget, envies) donnant lieu à des recommandations ciblées (vols, hébergements, activités).
- **Gestion de profil** : création de compte, préférences, suivi des voyages passés pour affiner les suggestions.
- **Intégration météo & localisation** : météo en temps réel, carte interactive, points d’intérêt et navigation.
- **Réservations & planning** : sélection de dates, réservation directe de vols, hôtels, activités.
- **Scanner QR / code-barres** : pour tickets, check-in, offres locales.
- **Formulaires interactifs** : mini-sondages ludiques pour enrichir les préférences.
- **Dark / Light mode automatique** : thème cohérent sur l’ensemble de l’app.
- **Multilingue FR / EN / AR** : strings localisés, support RTL automatique, changement à chaud via `LanguageManager`.

---

## Architecture & technique

| Côté iOS | Description |
| --- | --- |
| **Framework** | SwiftUI, async/await, URLSession |
| **Réseau** | `APIService` générique + `AuthService` / `UserService`, gestion d’erreurs (`NestError`) et Keychain (stockage JWT) |
| **UI** | Vues modulaires (Splash, Login, SignIn, OTP, Survey, Home, Profil, etc.), `FloatingTabBar` animée |
| **Localisation** | `LanguageManager` (EnvironmentObject) + ressources `Localizable.strings` FR/EN/AR |
| **Dark mode** | Couleurs unifiées via `ThemeColors` |
| **Validation** | Regex email / mot de passe, alertes contextualisées |

| Côté Backend | Description |
| --- | --- |
| **Framework** | NestJS (TypeScript) |
| **API REST** | Modules d’auth, utilisateur, réservation, paiement |
| **Sécurité** | JWT tokens, CORS, écoute sur `0.0.0.0` pour simulateur / device physique |
| **Persistance** | Mongoose/MongoDB (d’après stack fournie) |

---

## Prérequis

- macOS Sonoma / Sequoia, Xcode 16+ (ou 15.4 minimum), iOS Simulator ≥ 17.
- Node.js 18+ / npm 9+ pour le backend.
- Swift 5.9+ (livré avec Xcode).
- CocoaPods non nécessaire (Swift Package Manager uniquement).

---

## Installation & lancement

### Backend NestJS

```bash
git clone <repo-backend>
cd backend
npm install
npm run start:dev
```

Veillez à :
- Écouter sur `0.0.0.0` (`app.listen(3000, '0.0.0.0')`) pour autoriser le simulateur ou un iPhone physique (via l’IP du Mac).
- Activer CORS :
  ```ts
  app.enableCors({
    origin: ['http://127.0.0.1:3000', 'http://192.168.x.x:3000'],
    methods: 'GET,POST,PUT,PATCH,DELETE,OPTIONS',
    allowedHeaders: 'Content-Type,Authorization',
  });
  ```
- Renseigner une variable d’environnement `JWT_SECRET`, configurer MongoDB, etc.

### Application iOS

1. Cloner le dépôt WayFinder.
2. Ouvrir `WayFinder.xcodeproj` dans Xcode.
3. Mettre à jour `APIConfig.baseURL` si nécessaire :
   ```swift
   static let baseURL = URL(string: "http://127.0.0.1:3000/api")!
   ```
   - Simulateur : `127.0.0.1`.
   - Appareil physique : IP locale du Mac (ex. `http://192.168.0.42:3000/api`).
4. Sélectionner une cible (ex. *iPhone 16 Pro*), lancer `⌘R`.

---

## Configuration

### Côté iOS
- `LanguageManager` : stocke la langue via `UserDefaults`, force RTL pour l’arabe.
- `ThemeColors` : palette claire/sombre partagée (fonds, surfaces, textes, dégradés).
- `AuthService.login` : 
  1. POST `/auth/login` → récupération JWTil ne spkleffacer le cadre blanc sous le lil.
  2. Si le profil n’est pas renvoyé, fallback `UserService.shared.fetchProfile()` (GET `/api/user/profile`).
- `TokenStorage` : persistance du JWT en Keychain.

#### Social Login (Google & Apple)
> ℹ️ Google Sign-In côté iOS est désactivé tant que `/api/auth/google` n’est pas configuré dans le backend Render. Les instructions suivantes restent valables pour activer la fonctionnalité ultérieurement.

1. **Google**
   - Ajouter le package Swift Package Manager `https://github.com/google/GoogleSignIn-iOS` à la cible iOS (produit `GoogleSignIn`).
   - Créer un client OAuth iOS dans la console Google et récupérer :
     - `CLIENT_ID` (à placer dans `Info.plist` sous la clé `GOOGLE_CLIENT_ID`).
     - `nowREVERSED_CLIENT_ID` (à ajouter dans `CFBundleURLTypes` pour l’ouverture d’URL).
   - Si vous utilisez un fichier `GoogleService-Info.plist`, assurez-vous qu’il soit inclus dans la cible.
2. **Apple**
   - Activer la capability **Sign In with Apple** dans Xcode (Target ▸ Signing & Capabilities).
   - Vérifier que l’identifiant d’application est associé au service “Sign In with Apple” dans l’Apple Developer Portal.
3. **API**
   - Les routes `/api/auth/google` et `/api/auth/apple` doivent renvoyer la même structure que `/api/auth/login` (`access_token`, `user`, `onboarding_completed`).
4. **Tests rapides**
   - Démarrer le backend (`npm run start:dev`).
   - Lancer l’app (`⌘R`), cliquer sur « Continuer avec Google » ou « Continuer avec Apple ».
   - Vérifier dans les logs Xcode que le token est bien reçu et que l’écran `SurveyScreen` s’affiche.
   - Si la fenêtre Google ne s’ouvre pas, contrôler la présence du schéma d’URL inversé et du `GOOGLE_CLIENT_ID`.

### Côté NestJS
- `POST /api/auth/register` : doit accepter `username`, `email`, `first_name`, `last_name`, `password`.
- `POST /api/auth/login` : renvoie `accessToken`, `refreshToken?`, `user?`.
- `GET /api/user/profile` : renvoie le profil aligné sur `UserProfile`.
- Sécuriser les routes avec `AuthGuard('jwt')`.

---

## Localisation et RTL

- Fichiers `Localizable.strings` dans `Resources/Localization/{fr,en,ar}.lproj`.
- Les textes utilisent `LocalizedStringKey`.
- `LanguageSettingsView` permet de choisir la langue in-app.
- `LanguageManager` ajuste `Locale`, `LayoutDirection` et `UIView.appearance().semanticContentAttribute`.

---

## Mode sombre / clair

- Les vues lisent `@Environment(\.colorScheme)` et délèguent à `ThemeColors`.
- Pour tester :
  1. Dans le simulateur : `Features > Appearance > Light/Dark` (ou `⌘⇧A`).
  2. Dans les previews SwiftUI : menu déroulant “Appearance”.
- Aucun bouton de bascule, la palette suit automatiquement le thème système.

---

## Ethique & protection des données

WayFinder collecte des informations sensibles (profil, préférences). L’équipe doit :
- **Protéger la vie privée** : chiffrement, MFA, stockage sécurisé.
- **Être transparente** : expliquer comment les données alimentent les recommandations.
- **Être responsable** : conformité RGPD, droit d’accès/suppression, audits de sécurité.
- **Gérer les biais** : diversifier les datasets, surveiller la sortie des algorithmes.
- **Informer** : proposer un tableau de bord où l’utilisateur visualise / contrôle ses données.

---

## Améliorations recommandées

1. **Dashboard de données** : visualiser/modifier les données utilisées pour la personnalisation.
2. **Jeu de données diversifié** : réduire les biais d’IA.
3. **Sécurité renforcée** : chiffrement, MFA, détection d’intrusion.
4. **Transparence algorithmique** : expliquer les facteurs influençant les recommandations.
5. **Tests automatisés** : couvrir réseau, localisation, validations, changements de thème.
6. **Monitoring** : journaux anonymisés, audits réguliers, alertes en cas d’anomalie.

---

## Licence

Projet académique WayFinder – usage éducatif/démonstration. Vérifiez les licences des dépendances tierces avant déploiement commercial.
