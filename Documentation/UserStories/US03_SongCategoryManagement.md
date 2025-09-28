# US03: Song Category Management

## User Story
**As a** user, **I want** songs to be organized into "To Learn", "Learning", and "Learned" categories, **so that** I can track my progress and focus on songs appropriate to my current skill level.

## Context
- Categorization is fundamental to the learning algorithm
- Enables the app to present appropriate songs during flashcard sessions
- Provides structure for the learning progression

## Acceptance Criteria (Given–When–Then)
- **Given** I have added songs to my library, **when** I view my song library, **then** I should see songs organized into "To Learn", "Learning", and "Learned" categories.
- **Given** I have just added a new song, **when** I check my library, **then** the song should appear in the "To Learn" category.
- **Given** I am viewing my song library, **when** I tap on a category tab, **then** I should see only the songs in that category.
- **Given** I am viewing a specific category, **when** I long-press on a song, **then** I should have the option to manually move it to a different category.
- **Given** I have at least one song in my library, **when** I view the distribution of songs, **then** I should see how many songs are in each category.

## Technical Details
- Implement enum for learning status ("TO_LEARN", "LEARNING", "LEARNED")
- Create UserSong table in local database to track relationship between users and songs
- UserSong table should include learning status and tracking fields
- Create UI components for category-based navigation
- Implement repository pattern for data access with appropriate filtering

## Tasks / Sub-Tasks
- [ ] Design UI for category tabs/views in library
- [ ] Implement database structure for categorizing songs
- [ ] Create repository methods for filtering songs by category
- [ ] Implement manual category change functionality
- [ ] Add category distribution counter/visualization
- [ ] Ensure proper data synchronization between views

## Related Items
- Dependencies: US02 (Song Search and Addition)
- Related to Database schema design for UserSong table