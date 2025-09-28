# US04: Flashcard Session Initiation

## User Story
**As a** user, **I want** to start a flashcard session with songs from my library, **so that** I can begin learning to recognize songs.

## Context
- This is the entry point to the core learning functionality of the app
- Establishes the structure for how songs are presented to users
- Implements the session algorithm that selects which songs to present

## Acceptance Criteria (Given–When–Then)
- **Given** I have added songs to my library, **when** I tap "Start Flashcard Session", **then** a new session should begin.
- **Given** I have songs in different learning categories, **when** a flashcard session begins, **then** songs should be selected with roughly 70% from "Learning", 20% from "To Learn", and 10% from "Learned" categories.
- **Given** I have fewer than 10 songs in my "Learning" category, **when** a flashcard session begins, **then** additional songs from "To Learn" should be included to provide adequate content.
- **Given** I have started a flashcard session, **when** the session is underway, **then** I should see a clear indication of progress (e.g., "Song 3 of 10").
- **Given** I am in a flashcard session, **when** I want to end early, **then** I should have an option to exit the session.

## Technical Details
- Implement algorithm for selecting songs based on learning categories
- Create session state management that preserves progress if app is backgrounded
- Ensure proper song queue management
- Design flexible UI that accommodates various session lengths
- Consider using ViewModel for managing session state

## Tasks / Sub-Tasks
- [ ] Design session initiation UI
- [ ] Implement song selection algorithm based on category distribution
- [ ] Create session progress indicators
- [ ] Design and implement session navigation controls
- [ ] Add session state preservation
- [ ] Implement session exit functionality
- [ ] Add analytics for session metrics (optional)

## Related Items
- Dependencies: US03 (Song Category Management)
- Will be expanded by US05 (Song Playback Experience)