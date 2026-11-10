
-- V2: Du lieu mau cho Hoa Khanh Rental
-- Chi dung trong moi truong phat trien.
-- Tai khoan mau bi vo hieu hoa, khong dung de dang nhap.

-- 1. Tao hai tai khoan chu tro mau
INSERT INTO users (
    full_name,
    email,
    password_hash,
    phone,
    role,
    is_active
)
VALUES
    (
        'Chu tro mau A',
        'landlord-a@hoakhanh.example',
        'SEED_DISABLED_NO_LOGIN',
        NULL,
        'LANDLORD',
        FALSE
    ),
    (
        'Chu tro mau B',
        'landlord-b@hoakhanh.example',
        'SEED_DISABLED_NO_LOGIN',
        NULL,
        'LANDLORD',
        FALSE
    );

-- 2. Tao ho so chu tro
INSERT INTO landlord_profiles (
    user_id,
    contact_address,
    description
)
SELECT
    id,
    'Hoa Khanh, Da Nang (du lieu mau)',
    'Tai khoan chu tro dung de kiem thu'
FROM users
WHERE email IN (
                'landlord-a@hoakhanh.example',
                'landlord-b@hoakhanh.example'
    );

-- 3. Tao 6 phong tro mau
INSERT INTO rooms (
    landlord_id,
    title,
    description,
    address,
    area_name,
    price,
    area_sqm,
    max_occupants,
    moderation_status,
    occupancy_status,
    visibility_status
)
VALUES
    (
        (SELECT id FROM users
         WHERE email = 'landlord-a@hoakhanh.example'),
        'Phong tro mau gan Dai hoc Bach khoa',
        'Phong thoang mat, co cho de xe. Du lieu gia lap.',
        'Khu vuc Nguyen Luong Bang, Hoa Khanh, Da Nang',
        'Hoa Khanh',
        2500000,
        25,
        2,
        'APPROVED',
        'AVAILABLE',
        'VISIBLE'
    ),
    (
        (SELECT id FROM users
         WHERE email = 'landlord-a@hoakhanh.example'),
        'Phong tro mau gia sinh vien',
        'Phong don gian, phu hop sinh vien. Du lieu gia lap.',
        'Khu vuc Ton Duc Thang, Hoa Khanh, Da Nang',
        'Hoa Khanh',
        1800000,
        18,
        2,
        'APPROVED',
        'AVAILABLE',
        'VISIBLE'
    ),
    (
        (SELECT id FROM users
         WHERE email = 'landlord-a@hoakhanh.example'),
        'Phong tro mau co gac lung',
        'Phong co gac lung, khu bep rieng. Du lieu gia lap.',
        'Khu vuc Au Co, Hoa Khanh, Da Nang',
        'Hoa Khanh',
        3000000,
        30,
        3,
        'APPROVED',
        'AVAILABLE',
        'VISIBLE'
    ),
    (
        (SELECT id FROM users
         WHERE email = 'landlord-b@hoakhanh.example'),
        'Phong tro mau cho hai sinh vien',
        'Phong rong, gan khu vuc truong hoc. Du lieu gia lap.',
        'Khu vuc Me Suot, Hoa Khanh, Da Nang',
        'Hoa Khanh',
        2200000,
        22,
        2,
        'APPROVED',
        'AVAILABLE',
        'VISIBLE'
    ),
    (
        (SELECT id FROM users
         WHERE email = 'landlord-b@hoakhanh.example'),
        'Phong tro mau dang cho duyet',
        'Bai dang dung de kiem tra trang thai PENDING.',
        'Khu vuc Hoa Khanh, Da Nang',
        'Hoa Khanh',
        2000000,
        20,
        2,
        'PENDING',
        'AVAILABLE',
        'VISIBLE'
    ),
    (
        (SELECT id FROM users
         WHERE email = 'landlord-b@hoakhanh.example'),
        'Phong tro mau da cho thue',
        'Phong dung de kiem tra trang thai RENTED.',
        'Khu vuc Hoa Khanh, Da Nang',
        'Hoa Khanh',
        2800000,
        28,
        2,
        'APPROVED',
        'RENTED',
        'VISIBLE'
    );
