CREATE TABLE users (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER'
                             CONSTRAINT users_role_check CHECK (role IN ('ADMIN', 'CUSTOMER')),
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);
