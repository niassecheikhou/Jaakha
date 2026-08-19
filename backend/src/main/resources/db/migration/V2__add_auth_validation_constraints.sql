ALTER TABLE app_user
    ADD CONSTRAINT ck_app_user_full_name_min_length
    CHECK (char_length(trim(full_name)) >= 5) NOT VALID;

ALTER TABLE app_user
    ADD CONSTRAINT ck_app_user_email_format
    CHECK (email IS NOT NULL AND email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$') NOT VALID;

ALTER TABLE app_user
    ADD CONSTRAINT ck_app_user_senegal_phone
    CHECK (phone IS NOT NULL AND phone ~ '^\+221(70|71|76|77|78)[0-9]{7}$') NOT VALID;
