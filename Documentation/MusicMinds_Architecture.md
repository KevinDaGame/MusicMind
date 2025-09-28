# MusicMinds App Architecture

## Overview

MusicMinds is designed as a native Android application following modern architecture principles. The app uses a clean architecture approach with MVVM (Model-View-ViewModel) pattern to ensure separation of concerns, testability, and maintainability.

## Architecture Layers

The application is divided into the following layers:

### 1. Presentation Layer
- User interface components (Activities, Fragments)
- ViewModels
- UI state models
- Adapters and custom views

### 2. Domain Layer
- Use cases (business logic)
- Domain models
- Repository interfaces

### 3. Data Layer
- Repository implementations
- Data sources (local database, Spotify API)
- Data models and mappers

## Component Diagram

```mermaid
graph TD
    subgraph "Presentation Layer"
        UI[UI Components\nActivities/Fragments]
        VM[ViewModels]
        UI_State[UI State Models]
    end
    
    subgraph "Domain Layer"
        UC[Use Cases]
        DM[Domain Models]
        RI[Repository Interfaces]
    end
    
    subgraph "Data Layer"
        RP[Repository Implementations]
        DS_Local[Local Data Source]
        DS_Remote[Remote Data Source]
        Models[Data Models]
    end
    
    subgraph "External Services"
        Spotify[Spotify SDK]
        LocalDB[(Local Database)]
    end
    
    %% Connections
    UI <--> VM
    VM --> UI_State
    VM --> UC
    UC --> RI
    UC --> DM
    RI <-- DM --> RP
    RP --> DS_Local
    RP --> DS_Remote
    DS_Local --> LocalDB
    DS_Remote --> Spotify
    DS_Local --> Models
    DS_Remote --> Models
```

## Key Components

### ViewModels
- `FlashcardViewModel`: Manages flashcard session state and song playback
- `SongLibraryViewModel`: Manages song library and categorization
- `SearchViewModel`: Handles song search functionality
- `PlaylistImportViewModel`: Manages playlist import process
- `UserProgressViewModel`: Tracks user learning progress

### Use Cases
- `GetNextFlashcardUseCase`: Determines the next song for flashcard
- `VerifyAnswerUseCase`: Verifies user answers against song metadata
- `UpdateSongCategoryUseCase`: Updates song learning category
- `SearchSongsUseCase`: Searches for songs via Spotify
- `ImportPlaylistUseCase`: Imports songs from playlists

### Repositories
- `SongRepository`: Manages song data and operations
- `UserProgressRepository`: Manages user progress data
- `SpotifyRepository`: Handles Spotify API interactions

### Data Sources
- `SpotifyDataSource`: Interfaces with Spotify SDK
- `LocalSongDatabase`: Stores song metadata and learning state
- `UserPreferencesDataStore`: Manages user preferences

## Data Flow Diagram

```mermaid
sequenceDiagram
    participant User
    participant UI as UI Components
    participant ViewModel
    participant UseCase as Use Cases
    participant Repo as Repositories
    participant Local as Local Database
    participant Spotify as Spotify SDK
    
    %% Flashcard Flow
    User->>UI: Start flashcard session
    UI->>ViewModel: Request flashcard
    ViewModel->>UseCase: GetNextFlashcard()
    UseCase->>Repo: getNextSongToLearn()
    Repo->>Local: query songs by category
    Local-->>Repo: song data
    Repo-->>UseCase: song model
    UseCase-->>ViewModel: flashcard model
    ViewModel->>Spotify: play song clip
    ViewModel-->>UI: update UI state
    UI-->>User: display flashcard
    
    %% Answer Flow
    User->>UI: Submit answer
    UI->>ViewModel: verifyAnswer()
    ViewModel->>UseCase: VerifyAnswer()
    UseCase->>Repo: getSongDetails()
    Repo-->>UseCase: song details
    UseCase-->>ViewModel: verification result
    ViewModel->>UseCase: UpdateSongCategory()
    UseCase->>Repo: updateSongCategory()
    Repo->>Local: update song status
    ViewModel-->>UI: update UI with result
    UI-->>User: show answer feedback
```

## Technology Stack

### Core Framework
- Kotlin as primary language
- Android Jetpack components

### UI
- Material Design 3 components
- Jetpack Compose (optional)
- ViewBinding/DataBinding

### Asynchronous Programming
- Kotlin Coroutines
- Flow for reactive streams

### Dependency Injection
- Hilt or Koin

### Local Storage
- Room Database
- DataStore for preferences

### Remote Data
- Retrofit for additional API calls
- Spotify Android SDK

### Testing
- JUnit for unit testing
- Espresso for UI testing
- Mockk for mocking dependencies

## Module Structure

```
app/
├── di/                # Dependency injection modules
├── presentation/      # UI components
│   ├── flashcard/     # Flashcard feature
│   ├── search/        # Song search feature
│   ├── library/       # Song library management
│   └── settings/      # App settings
├── domain/            # Business logic and models
│   ├── usecase/       # Application use cases
│   ├── model/         # Domain models
│   └── repository/    # Repository interfaces
└── data/              # Data handling
    ├── repository/    # Repository implementations
    ├── local/         # Local data sources
    ├── remote/        # Remote data sources
    └── model/         # Data transfer objects
```

## State Management

The app uses unidirectional data flow:
1. UI events trigger ViewModel methods
2. ViewModels interact with Use Cases
3. Use Cases coordinate with Repositories
4. Repositories fetch/modify data from sources
5. Data flows back up the chain as state
6. UI observes and renders state changes

## Considerations for Future Expansion

- The architecture is designed to support additional features with minimal changes
- New song categorization mechanisms can be added by implementing new Use Cases
- Additional data sources can be added by implementing new Repository components
- The UI layer can be updated independently without affecting business logic
