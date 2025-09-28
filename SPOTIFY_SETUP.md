# Spotify SDK Setup Guide

## 1. Register Your App on Spotify Developer Dashboard

1. Go to [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)
2. Log in with your Spotify account
3. Click "Create App"
4. Fill in the app details:
   - **App Name**: MusicMinds
   - **App Description**: A music learning application with Spotify integration
   - **Redirect URI**: `musicminds://callback`
   - **Which API/SDKs are you planning to use**: Select "Android"

## 2. Configure Your App

After creating the app, you'll get a **Client ID**. You need to:

1. Copy your Client ID from the Spotify Developer Dashboard
2. Replace `"your_client_id_here"` in `MainActivity.kt` with your actual Client ID
3. Add your package name and app fingerprint (see step 3 below)

## 3. Add Package Name and Fingerprint

### Get your app's SHA-1 fingerprint:

**For debug builds (development):**
```bash
# On Windows (PowerShell):
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android

# On Windows (Command Prompt):
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

**For release builds (production):**
```bash
keytool -list -v -keystore path/to/your/release.keystore -alias your-alias-name
```

### Add to Spotify Dashboard:
1. In your Spotify app settings, scroll down to "Android Packages"
2. Click "Add Package"
3. Enter:
   - **Package Name**: `com.kevdadev.musicminds`
   - **SHA-1 Fingerprint**: The fingerprint you got from the keytool command

## 4. Current Configuration

Your app is configured with:
- **Redirect URI**: `musicminds://callback`
- **Package Name**: `com.kevdadev.musicminds`
- **SHA-1 Fingerprint (Debug)**: `A3:1B:35:83:96:FC:7D:E4:ED:9C:81:9B:E1:27:BE:03:64:DD:66:28`
- **Manifest Placeholders**: 
  - `redirectSchemeName`: "musicminds"
  - `redirectHostName`: "callback"

## 5. Testing

1. Install the Spotify app on your device
2. Make sure you're logged into Spotify
3. Run your MusicMinds app
4. The app should prompt for authorization and then connect to Spotify

## 6. Next Steps

Once basic connection is working, you can:
- Implement custom UI controls for playback
- Add search functionality
- Integrate with your flashcard system
- Handle offline scenarios
- Add error handling and user feedback

## Dependencies Added

- **Gson**: `com.google.code.gson:gson:2.10.1` (required by Spotify SDK)
- **Spotify App Remote SDK**: `spotify-app-remote-release-0.8.0.aar`
- **Spotify Auth SDK**: `spotify-auth-release-2.1.0.aar`

## Permissions

- `INTERNET`: Added to AndroidManifest.xml for network access