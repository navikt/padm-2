ALTER TABLE DIALOGMELDINGOPPLYSNINGER
    ADD COLUMN sent_to_arena BOOLEAN DEFAULT FALSE;

UPDATE dialogmeldingopplysninger
SET sent_to_arena = TRUE
WHERE arena IS NOT NULL;
