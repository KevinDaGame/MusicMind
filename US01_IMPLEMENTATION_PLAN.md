# US01 Implementation Plan: Spotify Authentication

## Current State Analysis
- ✅ **SDK Setup**: Spotify App Remote and Auth SDKs are integrated
- ✅ **Basic Connection**: App can detect Spotify and attempt connection 
- ✅ **Dashboard Config**: Client ID, package name, and fingerprint configured
- ❌ **Auth Flow**: Currently missing proper authentication flow implementation
- ❌ **Token Management**: No secure token storage or refresh mechanism
- ❌ **UI Flow**: No dedicated authentication screens

## Implementation Strategy

### Phase 1: Authentication Architecture Setup

#### 1.1 Create Authentication Components
- **AuthManager**: Central authentication coordinator
- **TokenManager**: Secure token storage using Android KeyStore
- **AuthRepository**: Data layer for auth operations
- **AuthViewModel**: UI state management for auth screens

#### 1.2 Define Authentication States
```kotlin
sealed class AuthState {
    object Unauthenticated : AuthState()
    object Authenticating : AuthState()
    data class Authenticated(val userInfo: SpotifyUser) : AuthState()
    data class AuthError(val error: String) : AuthState()
    object TokenExpired : AuthState()
}
```

#### 1.3 Create User Interface
- **SplashActivity**: Check auth state on app launch
- **AuthActivity**: Handle authentication flow
- **MainActivity**: Main app (auth-protected)

### Phase 2: Spotify Authorization Flow

#### 2.1 Implement AuthManager
```kotlin
class AuthManager(context: Context) {
    private val authRequest = AuthorizationRequest.Builder(
        CLIENT_ID,
        AuthorizationResponse.Type.TOKEN,
        REDIRECT_URI
    ).setScopes(arrayOf(
        "user-read-private",
        "user-read-email", 
        "streaming",
        "app-remote-control"
    )).build()
    
    // Methods:
    // - startAuthFlow()
    // - handleAuthResponse()
    // - refreshToken()
    // - logout()
}
```

#### 2.2 Secure Token Storage
```kotlin
class TokenManager(context: Context) {
    private val keyStore = AndroidKeyStore()
    
    // Methods:
    // - saveTokens(accessToken, refreshToken)
    // - getAccessToken()
    // - getRefreshToken()
    // - clearTokens()
    // - isTokenValid()
}
```

#### 2.3 Authentication Repository
```kotlin
class AuthRepository(
    private val authManager: AuthManager,
    private val tokenManager: TokenManager
) {
    // Methods:
    // - authenticate()
    // - refreshAccessToken()
    // - getUserInfo()
    // - logout()
    // - getAuthState()
}
```

### Phase 3: User Interface Implementation

#### 3.1 SplashActivity
- Check if user is authenticated
- Navigate to MainActivity if authenticated
- Navigate to AuthActivity if not authenticated
- Handle token refresh if needed

#### 3.2 AuthActivity
- Welcome screen with Spotify branding
- "Connect to Spotify" button
- Loading states during authentication
- Error handling with retry options
- Success confirmation

#### 3.3 Update MainActivity
- Remove current hardcoded auth attempt
- Use AuthViewModel to check auth state
- Display user info when authenticated
- Add logout option in menu

### Phase 4: Error Handling & Edge Cases

#### 4.1 Network Issues
- Offline detection
- Retry mechanisms
- User-friendly error messages

#### 4.2 Spotify App Issues
- App not installed handler
- App not logged in handler
- Version compatibility checks

#### 4.3 Token Management
- Automatic token refresh
- Expired token handling
- Invalid token recovery

### Phase 5: Testing & Polish

#### 5.1 Authentication Flow Testing
- Fresh install authentication
- Token refresh scenarios
- Logout and re-authentication
- Error recovery paths

#### 5.2 UI/UX Polish
- Loading animations
- Smooth transitions
- Accessibility support
- Error state designs

## File Structure

```
app/src/main/java/com/kevdadev/musicminds/
├── auth/
│   ├── AuthManager.kt
│   ├── TokenManager.kt
│   ├── AuthRepository.kt
│   ├── AuthViewModel.kt
│   └── data/
│       ├── AuthState.kt
│       └── SpotifyUser.kt
├── ui/
│   ├── auth/
│   │   ├── AuthActivity.kt
│   │   ├── AuthFragment.kt
│   │   └── AuthViewModel.kt
│   ├── splash/
│   │   ├── SplashActivity.kt
│   │   └── SplashViewModel.kt
│   └── main/
│       └── MainActivity.kt (updated)
└── utils/
    ├── Constants.kt
    └── Extensions.kt
```

## Technical Requirements

### Dependencies to Add
```kotlin
// In build.gradle.kts
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
implementation("androidx.navigation:navigation-fragment-ktx:2.7.4")
implementation("androidx.security:security-crypto:1.1.0-alpha06")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```

### Permissions Required
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

### Activities in AndroidManifest.xml
```xml
<activity android:name=".ui.splash.SplashActivity"
    android:theme="@style/SplashTheme"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>

<activity android:name=".ui.auth.AuthActivity"
    android:theme="@style/Theme.MusicMinds.Auth" />
```

## Acceptance Criteria Implementation

### AC1: First-time Launch Prompt
- SplashActivity checks auth state
- Redirects to AuthActivity if unauthenticated
- Shows welcome screen with "Connect to Spotify" button

### AC2: Spotify Login Direction
- AuthManager.startAuthFlow() opens Spotify authorization
- Uses Spotify's native auth flow
- Handles redirect back to app

### AC3: Authentication Success
- AuthManager processes authorization response
- TokenManager securely stores tokens
- Redirects to MainActivity with authenticated state

### AC4: Session Persistence
- TokenManager stores tokens in Android KeyStore
- SplashActivity checks stored tokens on startup
- Automatic token refresh before expiration

### AC5: Token Refresh
- AuthRepository monitors token expiration
- Automatic refresh using refresh token
- Fallback to re-authentication if refresh fails

### AC6: Disconnect Functionality
- Settings menu option to logout
- TokenManager.clearTokens()
- Redirect to AuthActivity

## Implementation Timeline

1. **Day 1-2**: Authentication architecture and data models
2. **Day 3-4**: Core authentication flow implementation
3. **Day 5-6**: UI screens and navigation
4. **Day 7**: Error handling and edge cases
5. **Day 8**: Testing and refinement

## Success Metrics

- ✅ User can successfully authenticate with Spotify
- ✅ Authentication persists between app sessions
- ✅ Token refresh works automatically
- ✅ Error states are handled gracefully
- ✅ User can disconnect and reconnect
- ✅ All acceptance criteria are met

---

**Ready for Review**: This plan provides a comprehensive, production-ready implementation of Spotify authentication following Android best practices and clean architecture principles.