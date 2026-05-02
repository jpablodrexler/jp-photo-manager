## ADDED Requirements

### Requirement: Log file output is structured JSON

The backend SHALL write all log events to a rolling file in JSON format using `logstash-logback-encoder`. Each log line SHALL be a single self-contained JSON object containing at minimum: `@timestamp`, `level`, `logger_name`, `message`, and any MDC fields present at the time of the event.

#### Scenario: Log file contains valid JSON lines
- **WHEN** the application emits a log event
- **THEN** the log file entry is a single line of valid JSON
- **AND** the entry contains `@timestamp`, `level`, `logger_name`, and `message` fields

#### Scenario: Log file rolls daily
- **WHEN** the log file reaches a new calendar day
- **THEN** the current log file is renamed with a date suffix
- **AND** a new log file is created at the configured path
- **AND** log files older than 30 days are deleted automatically

### Requirement: Console output remains human-readable

The backend SHALL write a human-readable plain-text representation of each log event to standard output (console). Console output SHALL NOT be JSON.

#### Scenario: Console output is plain text
- **WHEN** the application emits a log event
- **THEN** console output contains a human-readable line with timestamp, level, logger, and message
- **AND** the output is NOT JSON format

### Requirement: Log file location is documented

The README (`JPPhotoManagerWeb/README.md`) SHALL document the log file path (`~/.photomanager/logs/photomanager.log`), the JSON format, and the 30-day rolling retention policy.

#### Scenario: README contains logging section
- **WHEN** a user reads the README
- **THEN** they can find the log file location
- **AND** they can find the log format description
- **AND** they can find the retention policy
