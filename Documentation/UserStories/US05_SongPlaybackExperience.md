# US05: Song Playback Experience

## User Story
**As a** user, **I want** to hear full songs during flashcard sessions, **so that** I can test my ability to recognize songs by sound.

## Context
- This is the core content delivery mechanism of the application
- Audio playback is essential for the song recognition learning process
- Provides the stimulus that users will respond to in the learning flow

## Acceptance Criteria (Given–When–Then)
- **Given** I am in a flashcard session, **when** a new flashcard is presented, **then** the full song should automatically begin playing.
- **Given** a song is playing, **when** I want to control playback, **then** I should have options to pause, resume, and replay the song.
- **Given** a song is playing, **when** I try to seek or skip to different parts of the song, **then** this functionality should not be available (seeking is intentionally disabled).
- **Given** I am playing a full song, **when** the song reaches its end, **then** it should stop without automatically revealing the answer.
- **Given** I am playing a song, **when** I navigate away from the flashcard, **then** playback should stop.
- **Given** I am in a flashcard session, **when** audio playback fails for any reason, **then** I should receive an error message.

## Technical Details
- Implement Spotify SDK's playback functionality
- Handle audio focus changes (calls, other media apps)
- Create appropriate playback controls (play, pause, replay)
- Implement error handling for playback issues

## Tasks / Sub-Tasks
- [ ] Integrate Spotify SDK playback functionality for full songs
- [ ] Design and implement playback controls UI (play, pause, replay)
- [ ] Implement audio focus management
- [ ] Create error handling for playback failures
- [ ] Add visual feedback for audio state and progress

## Related Items
- Dependencies: US04 (Flashcard Session Initiation)
- Will be complemented by US06 (Answer Revelation)