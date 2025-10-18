# US02: Song Search and Addition

## User Story
**As a** user, **I want** to search for songs on Spotify and add them to my learning queue, **so that** I can build my personalized song library for flashcards.

## Context
- Users need a way to populate their song library before they can start learning
- Individual song search provides a precise way to add specific songs
- This feature establishes the initial content needed for the flashcard functionality

## Acceptance Criteria (Given–When–Then)
- **Given** I am authenticated with Spotify, **when** I navigate to "Add Songs", **then** I should see a search interface.
- **Given** I am on the search interface, **when** I enter a search term, **then** I should see relevant song results from Spotify.
- **Given** I am viewing search results, **when** I tap the "+" button next to a song, **then** it should be added directly to my "To Learn" category.
- **Given** I try to add a song that's already in my library, **when** I tap the "+" button, **then** I should be notified that the song already exists.
- **Given** I have a spotty internet connection, **when** I perform a search, **then** appropriate error handling should occur with user feedback.

## Technical Details

### Database Integration (SQLite)
- Use Room Database with SQLite backend for local storage
- Implement Song entity based on database schema:
  - `songId` (Primary Key - auto-generated UUID)
  - `spotifyId` (Unique - Spotify track identifier)
  - `title`, `artist`, `album`, `releaseYear`
  - `durationMs`, `previewUrl`, `imageUrl`
  - `genre` (optional), `trivia` (optional)
  - `dateAdded` (timestamp)
- Implement UserSong entity for learning relationship:
  - `userId`, `songId` (Composite Primary Key)
  - `learningStatus` (default: "TO_LEARN")
  - Initialize tracking fields: `correctAnswers=0`, `incorrectAnswers=0`
  - Set `statusChangedAt` to current timestamp

### API Integration
- Implement Spotify Search API through the Android SDK
- Map Spotify track objects to local Song entities
- Handle API rate limiting and error responses
- Use appropriate caching to reduce API calls
- Consider pagination for large result sets (default: 20 items per page)

### Data Validation
- Ensure spotifyId uniqueness before insertion
- Validate required fields (title, artist, spotifyId)
- Handle missing optional fields gracefully

### SQLite Queries (Room DAO methods)
```sql
-- Check if song already exists
SELECT COUNT(*) FROM Song WHERE spotifyId = ?

-- Insert new song
INSERT INTO Song (songId, spotifyId, title, artist, album, releaseYear, 
                  durationMs, previewUrl, imageUrl, dateAdded) 
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)

-- Add song to user's learning queue
INSERT INTO UserSong (userId, songId, learningStatus, statusChangedAt)
VALUES (?, ?, 'TO_LEARN', ?)

-- Search user's existing songs by title/artist
SELECT s.* FROM Song s 
JOIN UserSong us ON s.songId = us.songId 
WHERE us.userId = ? AND (s.title LIKE ? OR s.artist LIKE ?)
```

## Tasks / Sub-Tasks

### UI Implementation
- [ ] Design and implement search interface with search bar
- [ ] Create song list item layout showing title, artist, album art
- [ ] Add "+" quick-add button for each search result

### Database Implementation (SQLite + Room)
- [ ] Create Song entity with Room annotations
- [ ] Create UserSong entity with composite primary key
- [ ] Implement SongDao with insert, search, and duplicate check methods
- [ ] Create database migration scripts for initial schema
- [ ] Add unique constraint on Song.spotifyId
- [ ] Implement transaction handling for song addition

### API Integration
- [ ] Integrate with Spotify Search API
- [ ] Implement search result mapping to Song entity
- [ ] Create song preview functionality using previewUrl
- [ ] Add pagination support for search results
- [ ] Implement offline caching for search results

### Business Logic
- [ ] Implement duplicate detection using spotifyId
- [ ] Create quick-add functionality with UserSong creation
- [ ] Add proper error handling for network/database issues
- [ ] Optimize search response handling and UI feedback
- [ ] Add input validation and sanitization

## Related Items
- **Dependencies**: US01 (Spotify Authentication) - Required for API access
- **Database Schema**: References Song and UserSong entities in SQLite database
- **Architecture**: Follows Repository pattern with Room Database + SQLite
- **Follow-up Stories**: US03 (Song Category Management), US04 (Flashcard Sessions)