# Evaluation and improvements

**Origin:** Tunnel Vision was an **individual Brunel University project during 2022–23**. The original submitted design and implementation records are preserved separately; they have not been rewritten to suggest that later changes were present at submission.

## Evaluation findings

Reviewing the original software and manual results identified opportunities to strengthen testability, naming, logging, input handling and the reliability of calculations and error paths. The original manual test record reports **31 passed and one failed case out of 32**. The unsuccessful case documented a tendency to curve right during forward movement; steering commands were intentionally left unchanged.

## Improvements in the repository version

| Area | Evaluation finding and response |
|---|---|
| Project presentation | Replaced assignment-oriented `Task7` naming with `TunnelVisionApp`, clear console prompts and consistent package/class naming. |
| Image processing | Corrected grayscale pixel-read ordering and isolated arithmetic for synthetic-image tests. |
| Segment calculations | Separated timing and length checks, and clarified inclusive measurement boundaries. |
| Reporting | Corrected tunnel minimum/maximum selection and supported empty or partial journeys. |
| Command-line interaction | Consolidated repeated input validation while retaining user-facing prompt behaviour. |
| Detection | Isolated the light-transition state machine and reset pending transitions after failed image captures. |
| Calibration and camera failures | Added validity checks and bounded retries/polling between returned SwiftBot API calls. |
| File handling | Made output paths configurable and reported failed log writes instead of treating them as successful. |
| Shutdown paths | Attempted cleanup steps independently, including when a previous cleanup operation fails. |
| Testability | Added a robot adapter interface and desktop tests for calculations, sensor errors, and application failure branches. |

These are **post-evaluation improvements**, not features claimed to have passed the original 2023 on-device evaluation. Compilation and offline tests do not establish accurate measurement or safe operation on physical hardware.

## What was deliberately not changed

The original `startMove(40,40)` motor settings and the documented right-curving movement problem were **not corrected**. The original test outcomes and assignment requirements remain historical records. The repository also does not claim to enforce three completed tunnels, although the user is instructed to arrange at least three.

See [Testing](testing.md) and [Known limitations](known-limitations.md).
