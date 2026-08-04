-- Registration attempts live here until the emailed code is confirmed.
-- Previously register() wrote straight into `users` with email_verified =
-- false, so an account that never got verified sat in the real users table
-- forever, permanently claiming that email/username. Now nothing lands in
-- `users` until verifyEmail() succeeds - a fresh registration attempt for
-- the same email just overwrites the pending row here (see AuthService).
CREATE TABLE pending_registrations (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    favourite_club_id INT REFERENCES real_clubs(id) ON DELETE SET NULL,
    verification_code VARCHAR(6) NOT NULL,
    verification_code_expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
