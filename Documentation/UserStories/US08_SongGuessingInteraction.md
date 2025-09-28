# US08: Song Guessing Interaction

## User Story
**As a** user, **I want** to input my guesses for song title, artist, and year, **so that** I can actively test my knowledge.

## Context
- Active recall is a more effective learning method than passive recognition
- Input functionality makes the app more engaging and interactive
- Allows for specific tracking of which aspects of songs users are recognizing successfully

## Acceptance Criteria (Given–When–Then)
- **Given** I am in a flashcard session and a song is playing, **when** I want to guess the song details, **then** I should see input fields for title, artist, and year.
- **Given** I am viewing the input fields, **when** I enter my guesses, **then** the inputs should be clearly visible and editable.
- **Given** I am entering my guesses, **when** I start typing an artist or title, **then** I should see auto-complete suggestions to help with spelling variations.
- **Given** I have entered my guesses, **when** I tap "Submit", **then** my answers should be saved and ready for verification.
- **Given** I have started entering a guess, **when** I change my mind, **then** I should be able to clear my input and start over.
- **Given** I am entering a year, **when** I input data, **then** I should have appropriate controls for numerical input.
- **Given** I have not completed all input fields, **when** I tap "Submit", **then** I should be asked to confirm if I want to submit partial answers.

## Technical Details
- Design clean, accessible input UI that accommodates various text lengths
- Implement auto-complete/suggestions for artist and title fields
- Create appropriate input validation (especially for year field)
- Store user responses for comparison with correct answers
- Ensure keyboard handling is smooth on mobile devices
- Consider implementing voice input option for accessibility

## Tasks / Sub-Tasks
- [ ] Design input form UI for guessing
- [ ] Implement title and artist text input fields
- [ ] Create year input with appropriate controls
- [ ] Add auto-complete functionality
- [ ] Implement input validation
- [ ] Create clear/reset functionality
- [ ] Add submit button with confirmation dialog for partial answers
- [ ] Store responses for verification
- [ ] Test input handling with various text lengths and special characters

## Related Items
- Dependencies: US05 (Song Playback Experience)
- Will be enhanced by US09 (Answer Verification and Feedback)
- Should consider accessibility guidelines for input fields