ALTER TABLE courses
    ADD CONSTRAINT chk_courses_title_not_blank CHECK (char_length(btrim(title)) > 0) NOT VALID,
    ADD CONSTRAINT chk_courses_price_non_negative CHECK (price IS NULL OR price >= 0) NOT VALID,
    ADD CONSTRAINT chk_courses_currency_iso CHECK (currency IS NULL OR currency ~ '^[A-Z]{3}$') NOT VALID,
    ADD CONSTRAINT chk_courses_thumbnail_http CHECK (thumbnail_url IS NULL OR thumbnail_url ~ '^https?://.+') NOT VALID;

ALTER TABLE modules
    ADD CONSTRAINT chk_modules_title_not_blank CHECK (char_length(btrim(title)) > 0) NOT VALID,
    ADD CONSTRAINT chk_modules_sort_order_non_negative CHECK (sort_order IS NULL OR sort_order >= 0) NOT VALID;

ALTER TABLE lessons
    ADD CONSTRAINT chk_lessons_title_not_blank CHECK (char_length(btrim(title)) > 0) NOT VALID,
    ADD CONSTRAINT chk_lessons_type_valid CHECK (type IN ('VIDEO', 'TEXT', 'QUIZ')) NOT VALID,
    ADD CONSTRAINT chk_lessons_video_url_http CHECK (video_url IS NULL OR video_url ~ '^https?://.+') NOT VALID,
    ADD CONSTRAINT chk_lessons_duration_non_negative CHECK (duration_secs IS NULL OR duration_secs >= 0) NOT VALID,
    ADD CONSTRAINT chk_lessons_sort_order_non_negative CHECK (sort_order IS NULL OR sort_order >= 0) NOT VALID;

ALTER TABLE enrollments
    ADD CONSTRAINT chk_enrollments_status_valid CHECK (status IN ('ACTIVE', 'COMPLETED', 'REFUNDED')) NOT VALID,
    ADD CONSTRAINT chk_enrollments_amount_non_negative CHECK (amount_paid IS NULL OR amount_paid >= 0) NOT VALID;

ALTER TABLE lesson_progress
    ADD CONSTRAINT chk_lesson_progress_last_position_non_negative CHECK (last_position IS NULL OR last_position >= 0) NOT VALID;
