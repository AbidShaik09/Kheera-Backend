CREATE TABLE emails (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       recipient_email VARCHAR(255) UNIQUE NOT NULL,
                       is_urgent BOOLEAN,
                       subject VARCHAR(255),
                       body VARCHAR(500),
                       send_at TIMESTAMP ,
                       is_sent BOOLEAN,
                       is_failed BOOLEAN,
                       tries INT,
                       group_name VARCHAR(255),
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);