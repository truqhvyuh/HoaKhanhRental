CREATE TABLE IF NOT EXISTS schema_version (
    version INTEGER NOT NULL
);
INSERT INTO schema_version(version) VALUES (1);

CREATE TABLE users (
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
CREATE TABLE user_details (
    user_id INTEGER PRIMARY KEY REFERENCES users(id),
    display_name TEXT NOT NULL,
    phone TEXT,
    address TEXT,
    profile_picture TEXT,
    bio TEXT
);
CREATE TABLE admin_permissions (
    user_id INTEGER NOT NULL REFERENCES users(id),
    permission TEXT NOT NULL,
    PRIMARY KEY (user_id, permission)
);
CREATE TABLE sessions (
    token_hash TEXT PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id),
    created_at TEXT NOT NULL,
    last_used TEXT NOT NULL,
    expires_at TEXT NOT NULL,
    user_agent TEXT
);
CREATE TABLE pending_verifications (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    purpose TEXT NOT NULL,
    email TEXT NOT NULL,
    otp_hash TEXT NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_sent TEXT NOT NULL,
    expires_at TEXT NOT NULL,
    payload TEXT
);
CREATE TABLE teams (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE COLLATE NOCASE,
    leader_id INTEGER NOT NULL REFERENCES users(id),
    status TEXT NOT NULL DEFAULT 'active',
    created_at TEXT NOT NULL
);
CREATE TABLE organizations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE COLLATE NOCASE,
    description TEXT,
    owner_id INTEGER,
    contact_email TEXT,
    phone TEXT,
    address TEXT,
    status TEXT NOT NULL DEFAULT 'active',
    vacancy_deadline TEXT,
    created_at TEXT NOT NULL
);
CREATE TABLE group_memberships (
    user_id INTEGER PRIMARY KEY REFERENCES users(id),
    group_type TEXT NOT NULL,
    group_id INTEGER NOT NULL,
    joined_at TEXT NOT NULL
);
CREATE TABLE organization_member_permissions (
    organization_id INTEGER NOT NULL REFERENCES organizations(id),
    user_id INTEGER NOT NULL REFERENCES users(id),
    permission TEXT NOT NULL,
    PRIMARY KEY (organization_id, user_id, permission)
);
CREATE TABLE organization_former_members (
    organization_id INTEGER NOT NULL REFERENCES organizations(id),
    user_id INTEGER NOT NULL REFERENCES users(id),
    reason TEXT NOT NULL,
    at_time TEXT NOT NULL
);
CREATE TABLE listings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_type TEXT NOT NULL,
    owner_user_id INTEGER REFERENCES users(id),
    organization_id INTEGER REFERENCES organizations(id),
    posted_by INTEGER NOT NULL REFERENCES users(id),
    title TEXT,
    description TEXT,
    rent_type TEXT NOT NULL DEFAULT 'negotiable',
    rent_amount INTEGER,
    address TEXT,
    contact_email TEXT,
    contact_phone TEXT,
    contact_note TEXT,
    capacity_total INTEGER,
    capacity_available INTEGER,
    status TEXT NOT NULL DEFAULT 'draft',
    status_before_ban TEXT,
    poster_reassigned INTEGER NOT NULL DEFAULT 0,
    hold_by INTEGER,
    hold_reason TEXT,
    hold_time TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);
CREATE TABLE listing_attachments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    listing_id INTEGER NOT NULL REFERENCES listings(id),
    type TEXT NOT NULL,
    relative_path TEXT,
    original_size INTEGER NOT NULL,
    size INTEGER,
    status TEXT NOT NULL,
    failure_reason TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE applications (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    listing_id INTEGER NOT NULL REFERENCES listings(id),
    applicant_type TEXT NOT NULL,
    applicant_id INTEGER NOT NULL,
    status TEXT NOT NULL,
    note TEXT,
    party_size INTEGER NOT NULL DEFAULT 1,
    capacity_adjusted INTEGER NOT NULL DEFAULT 0,
    ended_at TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);
CREATE TABLE invites (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    group_type TEXT NOT NULL,
    group_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL REFERENCES users(id),
    status TEXT NOT NULL DEFAULT 'pending',
    note TEXT,
    expiry TEXT NOT NULL,
    created_at TEXT NOT NULL
);
CREATE TABLE join_requests (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    group_type TEXT NOT NULL,
    group_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL REFERENCES users(id),
    status TEXT NOT NULL DEFAULT 'pending',
    note TEXT,
    expiry TEXT NOT NULL,
    created_at TEXT NOT NULL
);
CREATE TABLE notifications (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id),
    type TEXT NOT NULL,
    notification_key TEXT,
    reference_kind TEXT,
    reference_id INTEGER,
    text_line TEXT NOT NULL,
    read_flag INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL
);
CREATE TABLE sanctions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id),
    type TEXT NOT NULL,
    reason TEXT,
    issued_by TEXT,
    start_time TEXT NOT NULL,
    end_time TEXT,
    revoked_time TEXT
);
CREATE TABLE audit_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    actor TEXT NOT NULL,
    action TEXT NOT NULL,
    target_type TEXT,
    target_id TEXT,
    detail TEXT,
    time TEXT NOT NULL
);
CREATE TABLE announcements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    author_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    pinned INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);
