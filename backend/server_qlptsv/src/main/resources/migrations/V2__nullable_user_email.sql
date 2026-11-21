PRAGMA foreign_keys = OFF;
CREATE TABLE users_v2 (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    login_name TEXT NOT NULL UNIQUE COLLATE NOCASE,
    email TEXT UNIQUE COLLATE NOCASE,
    email_verified INTEGER NOT NULL DEFAULT 0,
    password_hash TEXT NOT NULL,
    is_seed INTEGER NOT NULL DEFAULT 0,
    must_change_password INTEGER NOT NULL DEFAULT 0,
    must_change_email INTEGER NOT NULL DEFAULT 0,
    anonymized INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL
);
INSERT INTO users_v2(id,login_name,email,email_verified,password_hash,is_seed,must_change_password,must_change_email,anonymized,created_at)
SELECT id,login_name,NULLIF(email,''),email_verified,password_hash,is_seed,must_change_password,must_change_email,
       COALESCE(anonymized,0),created_at
FROM users;
DROP TABLE users;
ALTER TABLE users_v2 RENAME TO users;
CREATE UNIQUE INDEX users_email_nocase_unique
    ON users(email COLLATE NOCASE)
    WHERE email IS NOT NULL;
PRAGMA foreign_keys = ON;
