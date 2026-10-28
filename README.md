# Automated Parking System

Backend quản lý bãi đỗ xe tự động. Gate Simulator gửi sự kiện xe vào/ra; hệ thống phân bổ chỗ, quản lý phiên gửi xe và trả quyết định đồng bộ. Admin đăng nhập để theo dõi chỗ đỗ, tra cứu phiên và quản lý trạng thái bảo trì.

## Chức năng hiện có

- Check-in xe, chuẩn hóa biển số và tự động chọn chỗ phù hợp loại xe.
- Checkout xe, hoàn tất phiên gửi và giải phóng chỗ.
- Chống xử lý lặp bằng `eventId`; cùng event và payload trả lại kết quả cũ.
- Từ chối event dùng lại với payload khác.
- Đăng nhập Admin và phát hành JWT.
- Tra cứu, lọc và phân trang chỗ đỗ hoặc phiên gửi xe.
- Bật/tắt bảo trì cho chỗ đỗ.
- Ghi nhận lịch sử sự kiện cổng và quyết định nghiệp vụ trong PostgreSQL.

## API

| Method | Endpoint | Xác thực | Mục đích |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/login` | Public | Đăng nhập Admin và nhận JWT |
| `POST` | `/api/v1/gates/entry` | `X-Gate-Api-Key` | Check-in và phân bổ chỗ |
| `POST` | `/api/v1/gates/exit` | `X-Gate-Api-Key` | Checkout và giải phóng chỗ |
| `GET` | `/api/v1/parking-spots` | JWT `ADMIN` | Lọc và phân trang chỗ đỗ |
| `POST` | `/api/v1/parking-spots/{id}/maintenance` | JWT `ADMIN` | Bắt đầu bảo trì |
| `DELETE` | `/api/v1/parking-spots/{id}/maintenance` | JWT `ADMIN` | Kết thúc bảo trì |
| `GET` | `/api/v1/parking-sessions` | JWT `ADMIN` | Lọc và phân trang phiên gửi xe |
| `GET` | `/api/v1/parking-sessions/{id}` | JWT `ADMIN` | Xem chi tiết phiên gửi xe |

Hợp đồng request, response, status code và error code nằm tại [`src/main/resources/static/openapi.yaml`](src/main/resources/static/openapi.yaml). Khi ứng dụng chạy:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI: <http://localhost:8080/openapi.yaml>
- Health check: <http://localhost:8080/actuator/health>

## Kiến trúc

Luồng xử lý runtime:

```text
HTTP API -> Business Service -> Repository Port -> Persistence Adapter -> PostgreSQL
```

Hướng phụ thuộc mã nguồn:

```text
API ----------> Business <---------- Infrastructure
Controller      Domain, use case      Spring, Security, JPA/JDBC
DTO mapping     Port/interface        Adapter, transaction, config
```

Business Layer là Java thuần, không import Spring MVC, Spring Data, JPA hoặc Hibernate. API chỉ xử lý HTTP/validation và ánh xạ DTO. Infrastructure triển khai các port do Business định nghĩa, quản lý transaction, bảo mật và lưu trữ.

```text
src/main/java/com/ktpm/parking
├── api
│   ├── controller       # REST endpoints
│   ├── dto              # Request/response và ánh xạ HTTP
│   └── error            # Error response và exception mapping
├── business
│   ├── domain           # Domain model
│   ├── exception        # Lỗi nghiệp vụ
│   ├── port             # Port cho password/token
│   ├── repository       # Port truy cập dữ liệu/transaction
│   ├── service          # Use case và quy tắc nghiệp vụ
│   └── support          # Chuẩn hóa biển số, request hash
└── infrastructure
    ├── bootstrap        # Khởi tạo Admin đầu tiên
    ├── config           # Wiring Business Service và OpenAPI
    ├── persistence      # JPA/JDBC entity, mapper và adapter
    ├── security         # JWT, BCrypt và Gate API-key filter
    └── transaction      # Transaction boundary cho parking workflow
```

## Dữ liệu và tính nhất quán

- PostgreSQL 17, schema quản lý bằng Flyway.
- Migration hiện tại: V1 tạo schema, V2 seed chỗ đỗ, V3 thêm khóa biển số chuẩn hóa.
- Seed mặc định: 20 chỗ `MOTORBIKE` (`A01`–`A20`) và 10 chỗ `CAR` (`B01`–`B10`).
- Partial unique index ngăn một xe hoặc một chỗ có nhiều phiên `ACTIVE`.
- Check-in/checkout chạy trong transaction và sử dụng row/advisory lock.
- Kết quả Gate Event được lưu để hỗ trợ idempotent replay.
- Thời gian nghiệp vụ được ghi theo UTC.

## Công nghệ

- Java 21
- Spring Boot 3.5
- Maven 3.9+
- PostgreSQL 17, Flyway, Spring Data JPA và JDBC
- Spring Security, JWT HMAC-SHA256 và BCrypt
- OpenAPI/Swagger UI
- JUnit 5, Mockito và Testcontainers
- Docker Compose và GitHub Actions

## Chạy bằng Docker Compose

Yêu cầu: Docker Desktop hoặc Docker Engine có Docker Compose.

```bash
git clone https://github.com/Hiisam17/Automated_Parking_System.git
cd Automated_Parking_System
cp .env.example .env
```

Thay toàn bộ giá trị `replace-with-...` trong `.env`, sau đó chạy:

```bash
docker compose up -d --build
docker compose ps
```

Docker Compose tự đọc `.env`, khởi tạo PostgreSQL, chạy Flyway và khởi động backend sau khi database healthy.

Dừng dịch vụ nhưng giữ dữ liệu:

```bash
docker compose down
```

Lệnh sau xóa cả volume PostgreSQL và toàn bộ dữ liệu local:

```bash
docker compose down -v
```

## Biến môi trường

| Biến | Bắt buộc | Mô tả |
| --- | --- | --- |
| `POSTGRES_DB` | Docker Compose | Tên database |
| `POSTGRES_USER` | Docker Compose | User PostgreSQL |
| `POSTGRES_PASSWORD` | Có | Mật khẩu PostgreSQL |
| `DB_URL` | Khi chạy ngoài Compose | JDBC URL; mặc định `jdbc:postgresql://localhost:5432/automated_parking` |
| `DB_USERNAME` | Khi chạy ngoài Compose | User JDBC; mặc định `parking` |
| `DB_PASSWORD` | Khi chạy ngoài Compose | Mật khẩu JDBC |
| `GATE_API_KEY` | Có | API key cho Gate, tối thiểu 16 ký tự |
| `JWT_SECRET` | Có | Khóa ký JWT, tối thiểu 32 ký tự |
| `JWT_EXPIRATION_SECONDS` | Không | Thời hạn JWT; mặc định 3600 giây |
| `ADMIN_BOOTSTRAP_USERNAME` | Không | Username Admin được tạo ở lần khởi động đầu |
| `ADMIN_BOOTSTRAP_PASSWORD` | Không | Mật khẩu Admin ban đầu, lưu dưới dạng BCrypt |

Hai biến bootstrap Admin phải cùng có giá trị hoặc cùng để trống. Bootstrap không ghi đè tài khoản đã tồn tại; đổi mật khẩu trong `.env` không tự đổi mật khẩu của Admin trong database cũ.

Không commit `.env`. Repository chỉ lưu `.env.example` làm mẫu.

## Chạy bằng Maven

Yêu cầu: JDK 21+, Maven 3.9+ và PostgreSQL đang chạy.

Spring Boot không tự đọc file `.env`. Trên PowerShell, nạp biến trước khi chạy:

```powershell
Get-Content .env | Where-Object { $_ -and -not $_.StartsWith('#') } | ForEach-Object {
  $name, $value = $_ -split '=', 2
  Set-Item -Path "Env:$name" -Value $value
}

$env:DB_URL = "jdbc:postgresql://localhost:5432/$env:POSTGRES_DB"
$env:DB_USERNAME = $env:POSTGRES_USER
$env:DB_PASSWORD = $env:POSTGRES_PASSWORD
mvn spring-boot:run
```

## Kiểm thử

```bash
mvn clean verify
```

- Unit tests kiểm tra controller mapping, validation và Business Service.
- Integration tests dùng PostgreSQL thật qua Testcontainers để kiểm tra Flyway, persistence, transaction, locking và application context.
- Integration tests tự bỏ qua nếu Docker không khả dụng.

CI chạy cùng lệnh trên mọi push vào `basic_version`/`main` và trên pull request.
