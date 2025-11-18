# Wayfinder Android App

A modern Android travel planning application built with Jetpack Compose, featuring personalized recommendations, booking management, social features, and offline support.

## 🚀 Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Networking**: Retrofit 2 + OkHttp
- **Serialization**: Kotlinx Serialization
- **Image Loading**: Coil
- **Navigation**: Jetpack Navigation Compose
- **State Management**: ViewModel + StateFlow
- **Local Storage**: DataStore Preferences
- **Dependency Injection**: Manual (ViewModelFactory)
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36 (Android 15)
- **Compile SDK**: 36

## 📱 Features

### Core Features
- **Authentication**: Login, Registration, OTP Verification
- **Onboarding**: AI-driven dynamic questionnaire for personalized preferences
- **Home Screen**: Personalized flight recommendations, region-based filtering
- **Profile Management**: Edit profile, upload profile image, view booking history
- **Flight Search & Booking**: Browse flights, view details, book flights, payment integration (Flouci)

### Advanced Features
- **Favorites**: Save and manage favorite flights
- **Travel Itineraries**: Create and manage multi-day trip plans with activities
- **Reviews & Ratings**: Rate and review flights, hotels, and activities
- **Discussion Forum**: Create posts, comment, like, and engage with the community
- **Notifications**: In-app notifications with unread count badges
- **Search History**: Track recent searches and save searches for quick access
- **Price Alerts**: Set price alerts for flights and get notified when prices drop
- **Travel Tips**: AI-generated destination-specific travel tips by category
- **Offline Mode**: Download destinations for offline viewing
- **Social Features**: Follow users, share trips, view social feed

## 📁 Project Structure

```
app/src/main/java/tn/esprit/wayfinder/
├── data/                    # Data management (cache, offline storage)
├── manager/                 # Token and session management
├── models/                  # Data models (Auth, Booking, Flight, etc.)
├── network/                 # API service, Retrofit setup, interceptors
├── navigation/              # Navigation graph and routes
├── presentation/            # Repositories for data layer
│   ├── auth/
│   ├── booking/
│   ├── catalog/
│   ├── discussion/
│   ├── favorites/
│   ├── itinerary/
│   ├── notifications/
│   ├── pricealerts/
│   ├── reviews/
│   ├── searchhistory/
│   ├── social/
│   └── traveltips/
├── ui/
│   ├── components/          # Reusable UI components
│   ├── screens/             # All app screens
│   └── theme/               # Material Design theme
├── utils/                   # Utility functions (NetworkUtils, etc.)
└── viewmodels/              # ViewModels for state management
```

## 🛠️ Setup

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 8 or higher
- Android SDK 24+
- Gradle 8.0+

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd wayfinder-mobile-dam/androidstudio
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the `androidstudio` directory

3. **Sync Gradle**
   - Android Studio will automatically sync Gradle dependencies
   - Wait for the sync to complete

4. **Configure API Base URL** (if needed)
   - The base URL is configured in `network/RetrofitInstance.kt`
   - Default: `https://wayfinder-api-w92x.onrender.com/api/`

5. **Build and Run**
   - Connect an Android device or start an emulator
   - Click "Run" or press `Shift+F10`

## 🔧 Configuration

### API Configuration
The API base URL is set in `network/RetrofitInstance.kt`:
```kotlin
private const val BASE_URL = "https://wayfinder-api-w92x.onrender.com/api/"
```

### Authentication
- JWT tokens are automatically managed via `TokenManager`
- Tokens are stored securely using DataStore
- Auth interceptor adds Bearer token to all authenticated requests

### Image Loading
- Images are loaded using Coil with automatic caching
- Profile images and destination images are cached locally
- Offline images are supported via Coil's disk cache

## 📱 Screens

### Authentication & Onboarding
- **SplashScreen**: Initial app launch
- **LoginScreen**: User login
- **SignUpScreen**: User registration
- **VerificationScreenOTP**: OTP verification
- **SurveyScreen**: Onboarding questionnaire

### Main Screens
- **HomeScreen**: Personalized recommendations, region filters
- **AllFlightsScreen**: Browse all flights with advanced filters
- **FlightDetailsScreen**: Flight details, reviews, travel tips, price alerts
- **ProfileScreen**: User profile, navigation to all features
- **EditProfileScreen**: Edit user information and upload profile image

### Booking & Management
- **ReservationScreen**: Flight booking form
- **BookingHistoryScreen**: View all bookings
- **BookingDetailScreen**: Booking details
- **BookingConfirmationScreen**: Booking confirmation
- **FlouciPaymentScreen**: Payment processing

### Features
- **FavoritesScreen**: Manage favorite flights
- **ItineraryListScreen**: View all travel itineraries
- **ItineraryDetailScreen**: View itinerary details
- **CreateItineraryScreen**: Create new itinerary
- **DiscussionScreen**: Forum posts and discussions
- **PostDetailScreen**: Post details with comments
- **NotificationsScreen**: View notifications
- **SearchHistoryScreen**: Recent and saved searches
- **PriceAlertsScreen**: Manage price alerts
- **TravelTipsScreen**: View destination travel tips
- **OfflineDestinationsScreen**: View downloaded destinations

## 🔐 Security

- JWT token-based authentication
- Secure token storage using DataStore
- HTTPS-only API communication
- Input validation on all forms

## 📦 Dependencies

Key dependencies (see `app/build.gradle.kts` for full list):
- Jetpack Compose BOM
- Retrofit 2.9.0
- Kotlinx Serialization 1.6.0
- Coil 2.5.0
- DataStore Preferences 1.1.1
- Lifecycle ViewModel Compose 2.7.0
- Navigation Compose
- Material 3

## 🧪 Testing

Run tests:
```bash
./gradlew test
./gradlew connectedAndroidTest
```

## 🚀 Building

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

APK location: `app/build/outputs/apk/release/app-release.apk`

## 📝 Key Features Implementation

### Offline Mode
- Download destinations for offline viewing
- Access downloaded destinations from Profile screen
- Images cached automatically via Coil
- Data stored using DataStore Preferences

### Travel Tips
- AI-generated tips per destination
- Categorized tips (general, transportation, accommodation, food, culture, safety, budget, weather)
- Mark tips as helpful
- View tips from flight details screen

### Price Alerts
- Set price alerts for flights
- Get notified when prices drop below target
- Manage active alerts
- View alert history

### Search History
- Automatic tracking of recent searches
- Save searches for quick access
- Search statistics
- Clear recent searches

## 🐛 Troubleshooting

### Build Issues
- **Gradle sync fails**: Try "Invalidate Caches / Restart" in Android Studio
- **Kotlin version mismatch**: Check `gradle/libs.versions.toml`
- **Missing dependencies**: Run `./gradlew --refresh-dependencies`

### Runtime Issues
- **Network errors**: Check API base URL and internet connection
- **Authentication fails**: Verify JWT token is valid
- **Images not loading**: Check Coil configuration and image URLs

### Common Errors
- **"Unresolved reference"**: Clean and rebuild project
- **"Type mismatch"**: Check model definitions match backend API
- **Navigation errors**: Verify routes are registered in `NavHost.kt`

## 📄 License

Private project. All rights reserved.

## 👥 Contributors

Wayfinder Development Team

