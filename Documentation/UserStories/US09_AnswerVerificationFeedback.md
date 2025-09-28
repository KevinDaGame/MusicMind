# US09: Answer Verification and Feedback

## User Story
**As a** user, **I want** to check if my answers were correct, **so that** I know how well I'm learning each song.

## Context
- Feedback is essential to the learning process
- Verification allows users to see which aspects of songs they are recognizing successfully
- This feature bridges the gap between guessing and learning progress tracking
- Immediate feedback reinforces correct knowledge and highlights areas for improvement

## Acceptance Criteria (Given–When–Then)
- **Given** I have submitted my guesses for a song, **when** I tap "Check Answers", **then** I should see whether each guess (title, artist, year) is correct or incorrect.
- **Given** I am viewing my verified answers, **when** an answer is correct, **then** it should be visually indicated (e.g., green color, checkmark).
- **Given** I am viewing my verified answers, **when** an answer is incorrect, **then** it should be visually indicated (e.g., red color, X mark) and the correct answer should be shown.
- **Given** I have verified my answers, **when** the system determines the overall correctness, **then** my learning status for this song should be updated accordingly.
- **Given** I got the title and artist correct on first attempt, **when** the verification is complete, **then** the song should be considered for promotion to "Learned" status.
- **Given** I have verified my answers, **when** I was incorrect, **then** I should see helpful feedback about how close my answer was (e.g., "Almost! You were off by 2 years").
- **Given** I have completed the answer verification, **when** I'm ready to continue, **then** I should have a clear "Next" button to proceed to the next song.

## Technical Details
- Implement comparison logic for user guesses against correct answers
- Create tolerance rules for "close enough" answers (especially for years and spelling variations)
- Design visual feedback system for correct/incorrect answers
- Update song learning status based on performance
- Track correctness history for learning algorithm
- Store attempt results in UserSong database table

## Tasks / Sub-Tasks
- [ ] Design verification and feedback UI
- [ ] Implement answer comparison logic
- [ ] Create "close enough" rules for partial credit
- [ ] Design visual feedback indicators
- [ ] Implement learning status update logic
- [ ] Connect verification to database updates
- [ ] Add statistics tracking for correct/incorrect answers
- [ ] Create helpful feedback messages for incorrect answers
- [ ] Test with various input types and edge cases

## Related Items
- Dependencies: US08 (Song Guessing Interaction)
- Affects: US03 (Song Category Management)
- Will enhance the core learning algorithm