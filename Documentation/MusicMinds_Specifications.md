# MusicMinds App Specifications

## Overview
MusicMinds is an Android application designed to help users learn and memorize songs through an interactive flashcard system. The app plays song clips and challenges users to recall song titles, artists, and release years.

## Core Functionality

### Flashcard System
- The app plays a song clip
- Users guess three pieces of information:
  - Song title
  - Artist name
  - Release year
- Points are awarded for each correct answer
- After answering, correct information is displayed along with interesting trivia about the song or artist

### Learning Categories
Songs are organized into three categories:
1. **To Learn**: New songs not yet mastered
2. **Learning**: Songs in active rotation (in progress)
3. **Learned**: Songs mastered (consistently answered correctly)

### Learning Algorithm
- Maintain approximately 10 songs in "Learning" category at a time
- When fewer than 10 songs are in "Learning", songs from "To Learn" are promoted
- Occasional review of "Learned" songs to ensure retention
- If a "Learned" song is answered incorrectly, it's demoted to "Learning"
- Songs answered correctly on first attempt are fast-tracked to "Learned" status
- Multiple correct answers are required to promote a song from "Learning" to "Learned"

### Session Structure
- ~70% "Learning" songs
- ~20% new "To Learn" songs
- ~10% "Learned" song reviews

### User Verification (Initial Implementation)
- Self-reporting system where users indicate whether their answer was correct
- Correct answers are shown after response for verification

## Technical Requirements

### Audio Content
- Short song clips (15-30 seconds)
- Audio streaming or local storage capabilities
- Consideration of audio licensing requirements

### Spotify Integration
- Leverage Spotify Android SDK for song playback
- Key components:
  - **Authorization Library**: Handle user authentication and access token management
  - **App Remote Library**: Control playback in the Spotify app
- Implementation requirements:
  - Minimum Android SDK version 14
  - Gson dependency (version 2.6.1 or later)
  - Spotify app installed on user's device
- Features to implement:
  - Connect to Spotify app as a remote controller
  - Play song clips for flashcard challenges
  - Access song metadata (title, artist, year) for verification
  - Monitor playback state changes
  - Handle authorization flow for Spotify access
- Benefits:
  - Lightweight implementation (SDK <300kb)
  - Processing of playback and caching handled by Spotify app
  - Handles system integration (audio focus, lockscreen controls)
  - Works in both online and offline modes
  - Automatic track relinking for different regions

### Database
- Song metadata (title, artist, year, genre, etc.)
- User progress tracking
- Learning status for each song per user

### User Experience
- Clean, intuitive interface
- Quick response to user input
- Engaging feedback system

## Initial Implementation Focus
- Core flashcard functionality
- Basic learning algorithm
- Self-reporting accuracy system
- Limited song library to test concept
- Spotify integration for music playback
- Music import functionality

## Music Import Features

### Song Search Integration
- Implement a search interface using Spotify's Search API
- Display search results with essential metadata (title, artist, year)
- Provide preview snippets to verify the correct song
- Add a "+" button to quickly add songs to the learning queue
- Option to immediately categorize songs as "To Learn" upon adding

### Playlist Import
- Import songs from Spotify playlists (user's own playlists)
- Import songs from public Spotify playlists
- Preview playlist contents before confirming import
- Batch process multiple songs for efficient addition
- Add all imported songs to the "To Learn" category by default

## Spotify Implementation Steps

1. **Setup and Configuration**
   - Register app in Spotify Developer Dashboard to obtain Client ID
   - Add Spotify SDK dependencies to project
   - Configure authorization parameters and redirect URI

2. **User Authentication**
   - Implement Spotify authorization flow
   - Request necessary scopes for playback control
   - Handle access token management and refresh

3. **Connection Management**
   - Connect to Spotify App Remote
   - Handle connection lifecycle (connect/disconnect)
   - Implement connection state listeners

4. **Playback Control**
   - Play song clips with specified duration limits
   - Pause/resume functionality
   - Handle track progression for flashcard sequence

5. **Metadata Retrieval**
   - Access current track information (title, artist, album, year)
   - Retrieve additional track metadata for trivia content
   - Sync playback state with app state

6. **Error Handling**
   - Manage Spotify app not installed scenarios
   - Handle authentication failures
   - Address playback interruptions
