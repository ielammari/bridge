-- A candidate is told their own application went through. It is a routine
-- receipt rather than a decision, so it is silenceable, and a preference row
-- exists only for a silenceable type: the check widens to admit the new one.

ALTER TABLE preference_notification DROP CONSTRAINT ck_preference_type;

ALTER TABLE preference_notification ADD CONSTRAINT ck_preference_type CHECK (type_notification IN
    ('APPLICATION_RECEIVED', 'APPLICATION_SUBMITTED', 'SCHEDULE_NEEDED', 'INTERVIEW_SCHEDULED'));
