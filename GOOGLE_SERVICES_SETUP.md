# Google Services Setup Guide

## Current Status

A placeholder `google-services.json` file has been created to allow the project to build. **This is a temporary file and needs to be replaced with your actual Firebase configuration.**

## How to Get Your Real google-services.json File

### Step 1: Create/Open Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or select your existing project
3. Make sure the project name matches your app's needs

### Step 2: Add Android App to Firebase

1. In Firebase Console, click "Add app" and select Android
2. Enter your package name: `tn.esprit.wayFinder`
3. Register the app
4. Download the `google-services.json` file

### Step 3: Replace Placeholder File

1. Replace `/androidstudio/app/google-services.json` with the downloaded file
2. Make sure the package name in the file matches: `tn.esprit.wayFinder`

### Step 4: Configure Google Sign-In

1. In Firebase Console, go to **Authentication** → **Sign-in method**
2. Enable **Google** sign-in provider
3. Add your app's SHA-1 fingerprint:
   ```bash
   # Get your SHA-1 fingerprint
   cd androidstudio
   ./gradlew signingReport
   ```
4. Copy the SHA-1 from the debug keystore and add it to Firebase Console

### Step 5: Get OAuth Client ID

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select your Firebase project
3. Navigate to **APIs & Services** → **Credentials**
4. Find or create an OAuth 2.0 Client ID for Android
5. Update the `google_client_id_android` in `strings.xml` if needed

## Important Notes

- The placeholder file allows the build to succeed but **Google Sign-In and Firebase features will NOT work** until you replace it
- Keep the `google-services.json` file out of version control if it contains sensitive information (add to `.gitignore`)
- The current placeholder uses dummy values that won't work for actual authentication

## Verification

After replacing the file:
1. Clean and rebuild the project
2. Test Google Sign-In functionality
3. Verify Firebase Analytics and Messaging work (if used)

