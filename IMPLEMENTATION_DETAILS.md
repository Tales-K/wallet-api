# Requirements

- Given the importance of the project, I decided to skip authentication
  implementation in order to focus on consistency and performance.
- I found that idempotency is crucial for this project, so I implemented it
  using a combination of a unique request identifier and a database transaction.
- I designed the system as stateless, which allows for easy scaling and load
  balancing.
- Future cleanup schedules should be implemented to clean pending in_progress
  idempotency keys.
