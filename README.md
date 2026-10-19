# Automated Parking System

Backend nền tảng cho đồ án quản lý bãi đỗ xe tự động. Gate Simulator sẽ gửi sự kiện xe vào/ra; Admin theo dõi phiên gửi xe, trạng thái chỗ và bảo trì. Phase 1 chỉ dựng nền tảng kỹ thuật, schema và security; chưa triển khai nghiệp vụ check-in, checkout hay thuật toán phân bổ chỗ.

## Công nghệ

- Java 21
- Spring Boot 3.5.16 và Maven
- PostgreSQL 17, Spring Data JPA và Flyway
- Spring Security: JWT cho Admin, API key cho Gate Simulator
- Springdoc OpenAPI 2.8.17 và Swagger UI
- JUnit 5, Mockito và Testcontainers
- Docker, Docker Compose và GitHub Actions

## Yêu cầu cài đặt

- JDK 21 trở lên
- Maven 3.9 trở lên
- Docker Desktop hoặc Docker Engine có Docker Compose

## Clone và cấu hình

```bash
git clone https://github.com/Hiisam17/Automated_Parking_System.git
cd Automated_Parking_System
cp .env.example .env
```

Thay toàn bộ giá trị `replace-with-...` trong `.env`. Không commit `.env`. `JWT_SECRET` phải có ít nhất 32 ký tự, `GATE_API_KEY` ít nhất 16 ký tự. Nếu để trống cả hai biến `ADMIN_BOOTSTRAP_USERNAME` và `ADMIN_BOOTSTRAP_PASSWORD`, ứng dụng sẽ không tạo Admin ban đầu.

## Chạy PostgreSQL và backend bằng Maven

Khởi động riêng PostgreSQL:

```bash
docker compose up -d database
```

Nạp biến môi trường từ `.env`, sau đó chạy:

```bash
mvn spring-boot:run
```

Trên PowerShell có thể nạp biến bằng:

```powershell
Get-Content .env | Where-Object { $_ -and -not $_.StartsWith('#') } | ForEach-Object {
  $name, $value = $_ -split '=', 2
  Set-Item -Path "Env:$name" -Value $value
}
mvn spring-boot:run
```

## Chạy toàn bộ bằng Docker Compose

```bash
docker compose up --build
```

- Health: <http://localhost:8080/actuator/health>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI contract: <http://localhost:8080/openapi.yaml>

## Chạy kiểm thử

```bash
mvn clean verify
```

Unit test luôn chạy. Integration test dùng PostgreSQL thật qua Testcontainers và tự bỏ qua khi Docker không khả dụng.

## Cấu trúc project

```text
src/main/java/com/ktpm/parking
├── api                    # Controller, DTO, validation, exception handler ở Phase 2
├── business
│   ├── domain             # Domain model thuần Java
│   ├── repository         # Repository interface của Business Layer
│   └── support            # Quy tắc dùng chung không phụ thuộc framework
└── infrastructure
    ├── bootstrap          # Tạo Admin từ biến môi trường
    ├── persistence        # JPA entity, Spring Data, mapper, adapter
    └── security           # JWT và API key
src/main/resources
├── db/migration           # Schema và seed data Flyway
└── static/openapi.yaml    # Hợp đồng 8 API
```

Business Layer không phụ thuộc Spring MVC, Spring Data JPA, Hibernate hoặc PostgreSQL. JPA entity được tách khỏi domain model; adapter là cầu nối giữa repository interface và Spring Data.

## Đã setup

- Project Spring Boot có thể build và đóng gói bằng Maven.
- Schema cho `parking_spots`, `parking_sessions`, `gate_events`, `admin_accounts` với foreign key, check constraint và partial unique index.
- Seed 20 chỗ MOTORBIKE (`A01`–`A20`) và 10 chỗ CAR (`B01`–`B10`).
- Admin bootstrap an toàn từ biến môi trường, lưu mật khẩu BCrypt.
- Security nền tảng: API key cho `/api/v1/gates/**`, JWT role `ADMIN` cho API quản trị.
- Swagger UI đọc trực tiếp file OpenAPI đã thống nhất.
- Unit test, integration test Testcontainers, migration/context/health test, Docker Compose và CI.

## Chưa triển khai trong Phase 1

- Controller và xử lý `POST /api/v1/auth/login`.
- Check-in, checkout, chống xử lý lặp `eventId` và thuật toán phân bổ chỗ.
- API tra cứu, phân trang và quản lý bảo trì.
- Cơ chế phát hành JWT. Cấu hình hiện tại mới xác minh JWT HMAC do Phase 2 cung cấp.

Các endpoint nghiệp vụ trong OpenAPI vì vậy chưa trả dữ liệu giả. Nhóm tiếp tục code đúng contract tại `openapi.yaml`.

## Phân chia tham khảo

- Member 1: check-in, checkout, phân bổ chỗ và validation nghiệp vụ.
- Member 2: persistence adapter, transaction, constraint và integration test.
- Member 3: controller, DTO, đăng nhập/JWT, Admin API và Swagger.
