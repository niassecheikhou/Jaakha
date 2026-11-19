CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE session_token (
    id UUID PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES app_user(id),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE email_otp (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id),
    code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE provider_profile (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES app_user(id),
    photo_url VARCHAR(255),
    bio VARCHAR(1200),
    zone VARCHAR(255) NOT NULL,
    hourly_rate NUMERIC(10, 2) NOT NULL,
    experience_years INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    average_rating NUMERIC(3, 2) NOT NULL DEFAULT 0.00,
    review_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE provider_skill (
    provider_id UUID NOT NULL REFERENCES provider_profile(id) ON DELETE CASCADE,
    skill VARCHAR(255) NOT NULL
);

CREATE TABLE provider_availability (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL REFERENCES provider_profile(id) ON DELETE CASCADE,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL
);

CREATE TABLE booking (
    id UUID PRIMARY KEY,
    employer_id UUID NOT NULL REFERENCES app_user(id),
    provider_id UUID NOT NULL REFERENCES provider_profile(id),
    service_date DATE NOT NULL,
    start_time TIME NOT NULL,
    duration_hours INTEGER NOT NULL,
    address VARCHAR(255) NOT NULL,
    instructions VARCHAR(1200),
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE review (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL UNIQUE REFERENCES booking(id),
    employer_id UUID NOT NULL REFERENCES app_user(id),
    provider_id UUID NOT NULL REFERENCES provider_profile(id),
    rating INTEGER NOT NULL,
    comment VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE notification (
    id UUID PRIMARY KEY,
    recipient_id UUID NOT NULL REFERENCES app_user(id),
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1200) NOT NULL,
    booking_id UUID,
    user_id UUID,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE report (
    id UUID PRIMARY KEY,
    reporter_id UUID NOT NULL REFERENCES app_user(id),
    booking_id UUID REFERENCES booking(id),
    reason VARCHAR(255) NOT NULL,
    details VARCHAR(1200),
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_provider_profile_status ON provider_profile(status);
CREATE INDEX idx_provider_profile_zone ON provider_profile(zone);
CREATE INDEX idx_booking_status ON booking(status);
CREATE INDEX idx_booking_employer ON booking(employer_id);
CREATE INDEX idx_notification_recipient ON notification(recipient_id);
