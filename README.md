
# HoaKhanh Rental

Website tìm kiếm và quản lý phòng trọ cho sinh viên
khu vực Hòa Khánh, Đà Nẵng.

## Công nghệ

Backend:
- Java 21
- Spring Boot 4.1.2
- Spring Security + JWT
- PostgreSQL
- Flyway

Frontend:
- React
- TypeScript
- Vite
- React Router

## Cấu trúc

- backend/backend: Spring Boot
- frontend: React

## Chạy backend

1. Cài Java 21 và PostgreSQL.
2. Tạo database `hoakhanh_rental`.
3. Cấu hình hai biến môi trường bắt buộc:
   - `DB_PASSWORD`: mật khẩu PostgreSQL.
   - `JWT_SECRET`: khóa bí mật dùng để ký JWT.

4. Các biến môi trường tùy chọn:

   | Biến môi trường | Giá trị mặc định | Mô tả |
   |---|---|---|
   | `DB_URL` | `jdbc:postgresql://localhost:5432/hoakhanh_rental` | Địa chỉ kết nối PostgreSQL |
   | `DB_USERNAME` | `postgres` | Tên người dùng PostgreSQL |
   | `UPLOAD_DIR` | `uploads/rooms` | Thư mục lưu ảnh phòng |
   | `SHOW_SQL` | `false` | Đặt `true` để bật hiển thị SQL |

Windows CMD:

    set "DB_PASSWORD=your_db_password"
    set "JWT_SECRET=your_jwt_secret"

    cd backend\backend
    mvnw.cmd spring-boot:run

Nếu muốn sử dụng cấu hình riêng, có thể đặt thêm các biến
DB_URL, DB_USERNAME, UPLOAD_DIR và SHOW_SQL trước khi chạy.

Backend: http://localhost:8080

Flyway tự chạy migration khi khởi động.

Ghi chú: Hai biến DB_PASSWORD và JWT_SECRET vẫn bắt buộc như trước. Các biến còn lại có giá trị mặc định nên máy dev không cần khai báo nếu sử dụng cấu hình mặc định.

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
