
# HoaKhanh Rental

Website tìm kiếm và quản lý phòng trọ cho sinh viên
khu vực Hòa Khánh, Đà Nẵng.

## Công nghệ

Backend:
- Java 21
- Javalin 6.7.0
- Bcrypt 0.4
- SQLite 3.51.0

Frontend:
- React
- TypeScript
- Vite
- React Router

## Cấu trúc

- backend/backend: Javalin
- frontend: React

## Chạy backend

Cài Java 21, mở terminal tại thư mục gốc và chạy:

    java -jar backend\room-rental-backend.jar

API: http://127.0.0.1:7070. Nhấn `Ctrl+C` để dừng.

Xem [ghi chú vận hành server](backend/SERVER-OPERATIONS.md) để biết thêm về cấu hình và dữ liệu.

## Chạy frontend

Trong terminal khác:

    cd frontend
    npm ci
    npm run dev

Frontend: http://localhost:5173

Dùng Node.js tương thích với Vite trong package.json.

## Chức năng

STUDENT:
- Đăng ký, đăng nhập
- Tìm kiếm, lọc và xem phòng

LANDLORD:
- Đăng, sửa, ẩn và xóa mềm phòng
- Quản lý tình trạng thuê
- Upload và quản lý ảnh

ADMIN:
- Duyệt và từ chối tin đăng

## Phân quyền

Tài khoản mới mặc định có vai trò STUDENT.

Vai trò LANDLORD và ADMIN được cấp có kiểm soát
trong môi trường phát triển.

Không cho phép người dùng tự chọn quyền ADMIN.

## Quy trình làm việc nhóm

1. Clone repository.
2. Tạo branch feature/ten-chuc-nang.
3. Viết code, kiểm tra và commit.
4. Push branch.
5. Tạo Pull Request để review.
6. Merge vào main sau khi kiểm tra.

## Lưu ý

- Không commit mật khẩu, JWT secret hoặc dữ liệu thật.
- Không commit node_modules, build, dist, uploads.
- Không sửa các migration Flyway đã chạy.
- Chưa triển khai production.

## Việc nhóm tiếp tục phát triển

- Kiểm thử toàn diện frontend và backend
- Hoàn thiện responsive
- Cải thiện thông báo lỗi
- Bổ sung chức năng theo phân công
- Chuẩn bị triển khai production
