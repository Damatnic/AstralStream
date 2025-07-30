# Astral-Vu Development Plan

This document outlines the development plan for the Astral-Vu video player based on the initial project analysis. The plan is divided into several phases, each with specific goals and tasks.

## Phase 1: Project Stabilization

**Goal:** Create a stable foundation for future development by addressing critical build and dependency issues.

- **Task 1.1: Re-enable Hilt for Dependency Injection.**
  - Investigate and resolve the issues preventing Hilt from being used.
  - Refactor existing code to use Hilt for dependency injection where appropriate.
- **Task 1.2: Re-integrate Firebase and Google Services.**
  - Resolve any build conflicts related to these services.
  - Ensure that the project can be built and run with these services enabled.
- **Task 1.3: Establish Baseline Test Coverage.**
  - Set up a testing framework (e.g., JUnit, Mockito, Espresso).
  - Write unit tests for critical components like the `PlayerViewModel`.
  - Create basic UI tests to ensure the player launches and plays video.

## Phase 2: Core Feature Completion

**Goal:** Complete the implementation of partially implemented core player features.

- **Task 2.1: Fully Implement Picture-in-Picture (PiP) Mode.**
  - Implement the necessary logic to handle PiP mode transitions.
  - Ensure that playback continues smoothly in PiP mode.
- **Task 2.2: Integrate Horizontal Seek and Pinch-to-Zoom Gestures.**
  - Connect the existing gesture detection logic to the `PlayerViewModel`.
  - Implement the necessary UI feedback for these gestures.
- **Task 2.3: Implement Aspect Ratio Control.**
  - Connect the aspect ratio UI buttons to the player's rendering view.
  - Allow users to cycle through different aspect ratio modes (e.g., fit, fill, zoom).

## Phase 3: AI Feature Proof-of-Concept

**Goal:** Implement a proof-of-concept for one of the planned AI features to validate the approach and architecture.

- **Task 3.1: Choose an AI Feature for a PoC.**
  - Based on the existing code, "AI Subtitle Generation" is a good candidate.
- **Task 3.2: Replace Mock Implementation with a Real Implementation.**
  - Integrate a real speech-to-text library or API.
  - Implement the logic to process the audio from the video and generate subtitles.
- **Task 3.3: Display Generated Subtitles.**
  - Ensure that the generated subtitles can be displayed in the player.

## Phase 4: Code Quality and Refactoring

**Goal:** Improve the overall quality and maintainability of the codebase.

- **Task 4.1: Conduct a Comprehensive Code Review.**
  - Review the entire codebase for adherence to Kotlin best practices.
  - Identify and document areas for improvement.
- **Task 4.2: Refactor Large Components.**
  - Break down the `PlayerViewModel` into smaller, more manageable components.
  - Improve the separation of concerns between UI and business logic.
- **Task 4.3: Add Documentation.**
  - Add KDoc comments to all public classes and methods.
  - Update the project's README with build instructions and an overview of the architecture.

## Proposed Timeline

- **Phase 1:** 1-2 weeks
- **Phase 2:** 2-3 weeks
- **Phase 3:** 2-4 weeks
- **Phase 4:** Ongoing

This plan provides a clear path forward for the Astral-Vu project. By focusing on stabilization, feature completion, and code quality, we can create a robust and maintainable video player application.