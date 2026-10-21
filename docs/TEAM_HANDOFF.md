# AUTOMATED PARKING SYSTEM

## Tài liệu thống nhất triển khai nghiệp vụ

- **Phiên bản:** 1.1
- **Ngày:** 21/10/2026
- **Đối tượng:** Nhóm 3 thành viên
- **Nhánh nền:** `basic_version`

Mục tiêu của tài liệu này là giúp mỗi thành viên nhận việc, tạo nhánh và triển khai mà không phải tự đoán hợp đồng của người khác.

> Đây là tài liệu thiết kế cho giai đoạn triển khai nghiệp vụ tiếp theo, không phải mô tả rằng các API đã được lập trình. Baseline hiện tại mới hoàn thành scaffold kỹ thuật, schema ban đầu, security nền tảng, Docker, CI và OpenAPI.

## 1. Nguồn sự thật và quy tắc thay đổi

Thứ tự ưu tiên khi có khác biệt:

1. `src/main/resources/static/openapi.yaml` là hợp đồng HTTP chính thức.
2. Tài liệu này là hợp đồng phân tầng, phân công và nghiệp vụ nội bộ.
3. Flyway migration là lịch sử schema đã được chia sẻ.
4. README là hướng dẫn chạy dự án.

Không tự ý thay đổi đường dẫn API, JSON, enum, repository port hoặc schema trên một nhánh cá nhân. Thay đổi contract phải được đưa qua PR và có review của thành viên liên quan.

Các file Flyway đã tồn tại:

- `V1__create_parking_schema.sql`: tạo schema ban đầu.
- `V2__seed_parking_spots.sql`: seed 30 chỗ đỗ.

Vì vậy, thay đổi schema tiếp theo phải bắt đầu từ `V3__...sql`. Không sửa V1/V2 sau khi chúng đã được chia sẻ và chạy trên máy thành viên khác.

## 2. Sản phẩm và phạm vi

Hệ thống nhận sự kiện từ Gate Simulator, tự xử lý check-in, phân bổ chỗ, checkout và giải phóng chỗ. Người gửi xe không có tài khoản và không trực tiếp gọi API.

Hai tác nhân:

- **Gate Simulator:** gửi sự kiện ENTRY/EXIT bằng API key.
- **Admin:** đăng nhập bằng username/password, nhận JWT để xem chỗ, xem phiên gửi xe và quản lý bảo trì.

Ngoài phạm vi:

- Camera và barie thật.
- Staff/User, đăng ký tài khoản, đặt chỗ, thanh toán hoặc mã nhận xe.
- API thêm/xóa chỗ đỗ.
- Dashboard tổng quan riêng.

Số lượng chỗ cố định và được tạo bởi Flyway: `A01-A20` cho xe máy, `B01-B10` cho ô tô.

## 3. Quyết định kiến trúc bắt buộc

| Mã | Quyết định | Người chịu trách nhiệm chính |
|---|---|---|
| D01 | Layered Monolith: `api -> business -> infrastructure -> PostgreSQL`. | Cả nhóm |
| D02 | Business dùng Java thuần, không import Spring MVC, Spring Data, JPA hoặc Hibernate. | Member 1 |
| D03 | Java 21, Spring Boot 3.x, Maven, PostgreSQL, Flyway, JPA, JWT, API key, Swagger, Docker Compose, JUnit 5 và Testcontainers. | Member 2 + 3 |
| D04 | Chỉ triển khai 8 operation đã định nghĩa trong OpenAPI. | Member 3 |
| D05 | `VehicleType`: `MOTORBIKE`, `CAR`; spot: `AVAILABLE`, `OCCUPIED`; session: `ACTIVE`, `COMPLETED`. | Member 1 + 2 |
| D06 | `inMaintenance` độc lập với trạng thái chiếm dụng. Không đổi `OCCUPIED` bằng tay. | Member 2 + 3 |
| D07 | Chọn chỗ phù hợp theo `code ASC` trong phiên bản đầu tiên. | Member 1 + 2 |
| D08 | Một biển số tối đa một session ACTIVE; một spot tối đa một session ACTIVE. Business kiểm tra và DB là lớp bảo vệ cuối cùng. | Member 1 + 2 |
| D09 | `eventId` duy nhất toàn hệ thống, kể cả ENTRY và EXIT. Retry phải giữ nguyên `eventId`. | Cả nhóm |
| D10 | GateEvent, ParkingSpot và ParkingSession của một ENTRY/EXIT thay đổi trong cùng một transaction. Lỗi kỹ thuật phải rollback. | Member 2 |
| D11 | Gate dùng `X-Gate-Api-Key`; Admin dùng JWT có role `ADMIN`. Không hardcode secret. | Member 3 |
| D12 | Thời gian lấy từ backend qua `Clock`, biểu diễn bằng `OffsetDateTime` ở UTC và lưu `timestamptz`. | Member 1 + 2 |
| D13 | Danh sách dùng `page=0`, `size=20`, tối đa `100`. Session sắp xếp `checkInAt DESC, id DESC`; spot theo `code ASC`. | Member 2 + 3 |
| D14 | Check-in/checkout xử lý đồng bộ; Gate nhận quyết định cuối cùng trước khi mô phỏng cho xe qua. | Member 1 + 3 |

## 4. Chuẩn hóa biển số

Nhóm thống nhất hai giá trị:

- `licensePlate`: dùng hiển thị; trim, uppercase và chuẩn hóa khoảng trắng.
- `licensePlateKey`: dùng so sánh duy nhất; uppercase rồi loại bỏ toàn bộ khoảng trắng và dấu gạch nối.

Ví dụ `29A-12345`, `29a 12345` và ` 29A-12345 ` đều có key `29A12345`.

`licensePlateKey` phải được dùng nhất quán trong ENTRY, EXIT, unique index và query. Baseline hiện tại chưa có cột này và normalizer hiện tại mới loại bỏ khoảng trắng; PR contract đầu tiên phải cập nhật OpenAPI, normalizer, domain/entity/mapper và thêm migration V3 trước khi ghép nghiệp vụ thật.

## 5. Danh sách API chính thức

| API | Quyền | Chủ trì | Kết quả chính |
|---|---|---|---|
| `POST /api/v1/auth/login` | Public | M3 | Trả `accessToken`, `tokenType`, `expiresIn`. |
| `POST /api/v1/gates/entry` | Gate API key | M1 + M3 | Check-in, cấp chỗ; thành công `201`. |
| `POST /api/v1/gates/exit` | Gate API key | M1 + M3 | Checkout, giải phóng chỗ; thành công `200`. |
| `GET /api/v1/parking-spots` | Admin JWT | M3 | Lọc và phân trang danh sách chỗ. |
| `POST /api/v1/parking-spots/{id}/maintenance` | Admin JWT | M3 | Bật bảo trì; thành công `201`. |
| `DELETE /api/v1/parking-spots/{id}/maintenance` | Admin JWT | M3 | Tắt bảo trì idempotent; thành công `204`. |
| `GET /api/v1/parking-sessions` | Admin JWT | M3 | Lọc và phân trang các phiên gửi xe. |
| `GET /api/v1/parking-sessions/{id}` | Admin JWT | M3 | Xem chi tiết phiên; không thấy trả `404`. |

Response lỗi chuẩn:

```json
{
  "error": {
    "code": "NO_AVAILABLE_SPOT",
    "message": "Không còn chỗ phù hợp với loại xe."
  }
}
```

Các mã lỗi chính:

- `VALIDATION_ERROR` — 400.
- `UNAUTHORIZED`, `INVALID_CREDENTIALS` — 401.
- `FORBIDDEN` — 403.
- `SPOT_NOT_FOUND`, `SESSION_NOT_FOUND` — 404.
- `VEHICLE_ALREADY_PARKED`, `NO_AVAILABLE_SPOT`, `NO_ACTIVE_SESSION`, `EVENT_ID_REUSED`, `SPOT_OCCUPIED`, `ALREADY_IN_MAINTENANCE` — 409.
- `INTERNAL_ERROR` — 500.
- `SERVICE_UNAVAILABLE` — 503.

Domain service trả mã nghiệp vụ; API layer quyết định HTTP status và DTO. Controller không trả JPA Entity.

## 6. Luồng nghiệp vụ

### 6.1. ENTRY

1. Gate gửi `eventId`, `gateId`, `licensePlate`, `vehicleType` cùng API key.
2. API kiểm tra cấu trúc request; Business chuẩn hóa biển số và tạo request hash ổn định từ `(eventType, gateId, licensePlateKey, vehicleType)`.
3. Nếu `eventId` đã hoàn tất với cùng hash, trả lại cùng response và thêm `X-Event-Replayed: true`.
4. Nếu `eventId` đã tồn tại với hash khác, trả `409 EVENT_ID_REUSED`.
5. Kiểm tra xe chưa có session ACTIVE.
6. Khóa và chọn spot đúng loại, `AVAILABLE`, `inMaintenance=false`, ưu tiên `code ASC`.
7. Tạo session ACTIVE, chuyển spot sang OCCUPIED và lưu kết quả GateEvent trong cùng transaction.
8. Trả `201` với `status`, `sessionId`, `assignedSpot`.

Nếu xe đã ở trong bãi hoặc không còn chỗ phù hợp, trả 409 và lưu quyết định từ chối của event để retry không tự đổi kết quả.

### 6.2. EXIT

1. Gate gửi `eventId`, `gateId`, `licensePlate` cùng API key.
2. Áp dụng cùng quy tắc event replay/hash như ENTRY.
3. Tìm và khóa session ACTIVE theo `licensePlateKey`.
4. Không có session ACTIVE thì trả `409 NO_ACTIVE_SESSION` và lưu quyết định từ chối.
5. Chuyển session sang COMPLETED, ghi `checkOutTime`, chuyển spot về AVAILABLE và lưu GateEvent trong cùng transaction.
6. Trả `200` với `status`, `sessionId`, `releasedSpot`.

### 6.3. Bảo trì

- Chỉ bật bảo trì khi spot AVAILABLE và chưa bảo trì.
- Bật bảo trì cho spot OCCUPIED trả `409 SPOT_OCCUPIED`.
- Bật lại spot đã bảo trì trả `409 ALREADY_IN_MAINTENANCE`.
- DELETE bảo trì trả `204` kể cả spot vốn đã mở.
- Bảo trì và check-in phải dùng cùng cơ chế lock/conditional update để chỉ một thao tác thành công.

## 7. Hợp đồng Business Layer

Package chính thức là `com.ktpm.parking`, theo cấu trúc hiện có:

```text
com.ktpm.parking
├── api
│   ├── controller
│   ├── dto
│   └── error
├── business
│   ├── domain
│   ├── exception
│   ├── repository
│   ├── service
│   └── support
└── infrastructure
    ├── bootstrap
    ├── persistence
    ├── security
    └── transaction
```

Giữ tên type hiện có:

- `VehicleType`
- `ParkingSpotStatus`
- `ParkingSessionStatus`
- `GateEventType`
- `GateEventProcessingStatus`
- `ParkingSpot`, `ParkingSession`, `GateEvent`, `AdminAccount`

Domain dùng `Long`, `OffsetDateTime` và tên nội bộ `checkInTime`/`checkOutTime`. API layer map sang `checkInAt`/`checkOutAt` theo OpenAPI.

### 7.1. Service contract — Member 1

```java
public interface ParkingService {
    GateResult checkIn(CheckInCommand command);
    GateResult checkOut(CheckOutCommand command);
}

public record CheckInCommand(
        String eventId,
        String gateId,
        String licensePlate,
        VehicleType vehicleType) {}

public record CheckOutCommand(
        String eventId,
        String gateId,
        String licensePlate) {}

public record GateDecision(
        boolean accepted,
        String code,
        String message,
        Long sessionId,
        String spotCode) {}

public record GateResult(GateDecision decision, boolean replayed) {}
```

`GateResult.replayed` cho phép Member 3 trả header `X-Event-Replayed`. Các type Business không chứa HTTP status hoặc DTO của API.

### 7.2. Repository contract mục tiêu — Member 2 triển khai

Baseline đã có các repository CRUD dùng cho scaffold. Workflow ENTRY/EXIT dùng các port riêng để không buộc adapter hiện tại giả lập locking bằng `findAll`/`save`:

```java
public interface ParkingSpotWorkflowRepository {
    Optional<ParkingSpot> lockFirstAvailable(VehicleType vehicleType);
    boolean markOccupied(Long spotId);
    boolean markAvailable(Long spotId);
}

public interface ParkingSessionWorkflowRepository {
    boolean existsActiveByLicensePlate(String licensePlateKey);
    Optional<ParkingSession> lockActiveByLicensePlate(String licensePlateKey);
    ParkingSession createActive(
            String licensePlate,
            String licensePlateKey,
            VehicleType vehicleType,
            Long spotId,
            OffsetDateTime checkInTime);
    boolean complete(Long sessionId, OffsetDateTime checkOutTime);
}

public interface ParkingUnitOfWork {
    <T> T execute(Supplier<T> action);
}
```

Các phương thức lock/update phải chạy trong transaction của `ParkingUnitOfWork`. Conditional update phải chỉ thành công khi đúng trạng thái mong đợi; kết quả bất thường phải rollback thay vì commit dữ liệu nửa vời.

Các query phân trang của Admin nên dùng port riêng do Member 2 và 3 thống nhất. Controller không gọi thẳng Spring Data Repository.

### 7.3. Event replay contract

```java
public interface GateEventWorkflowRepository {
    EventStart beginOrReplay(
            String eventId,
            GateEventType eventType,
            String requestHash);
    void saveResult(String eventId, GateDecision result);
}

public sealed interface EventStart
        permits EventStart.New, EventStart.Replay, EventStart.Reused {
    record New() implements EventStart {}
    record Replay(GateDecision previous) implements EventStart {}
    record Reused() implements EventStart {}
}
```

- `New`: event mới, tiếp tục xử lý.
- `Replay`: cùng ID và cùng hash, trả quyết định cũ, không thay đổi spot/session.
- `Reused`: cùng ID nhưng hash khác, trả `EVENT_ID_REUSED`.

Hai request đồng thời cùng `eventId` phải được xử lý an toàn bằng unique constraint kết hợp PostgreSQL locking/upsert phù hợp. Không bắt unique violation rồi tiếp tục dùng một transaction đã bị PostgreSQL đánh dấu lỗi.

`gate_events` tiếp tục dùng tên cột hiện tại: `request_hash`, `processing_status`, `response_data`. Không bắt buộc thêm `http_status` vì ánh xạ là cố định: ENTRY thành công 201, EXIT thành công 200, từ chối nghiệp vụ 409. Nếu nhóm mở rộng thêm status trong tương lai, thay đổi này phải qua migration và contract PR.

## 8. Thay đổi schema bắt buộc trước khi ghép nghiệp vụ

Tạo migration mới, ví dụ:

```text
V3__add_license_plate_key.sql
```

Migration phải:

1. Thêm `license_plate_key` vào `parking_sessions`.
2. Backfill an toàn nếu database đã có session.
3. Đặt `NOT NULL` sau khi backfill.
4. Thay unique index ACTIVE theo `license_plate` bằng index theo `license_plate_key`.
5. Bổ sung index phục vụ truy vấn session theo key nếu cần.

Index mục tiêu:

```sql
CREATE UNIQUE INDEX uq_parking_sessions_active_license_plate_key
    ON parking_sessions (license_plate_key)
    WHERE status = 'ACTIVE';

CREATE UNIQUE INDEX uq_parking_sessions_active_spot
    ON parking_sessions (parking_spot_id)
    WHERE status = 'ACTIVE';
```

Không tự động xóa session cũ. Lỗi kỹ thuật 5xx phải rollback và không được đánh dấu là kết quả cuối cùng; Gate có thể retry cùng `eventId`.

## 9. Phân công

### Member 1 — Business / Check-in / Checkout

Sở hữu:

- `ParkingService`, command/result và business exception.
- Chuẩn hóa biển số và validation nghiệp vụ.
- Điều phối ENTRY/EXIT qua repository ports và `ParkingUnitOfWork`.
- Chọn chỗ đầu tiên thông qua `lockFirstAvailable`.
- Unit test thuần Java cho các nhánh accept/reject/replay.

Không làm REST Controller, Spring Security, JPA Entity, SQL locking hoặc migration.

Nhánh đề xuất: `feature/core-business`.

### Member 2 — Database / Repository / Transaction

Sở hữu:

- Migration V3 trở đi, JPA Entity, mapper và adapter.
- Repository locking/conditional update.
- `ParkingUnitOfWork` và transaction thật.
- Gate event replay an toàn khi concurrent.
- Constraint/index và integration test PostgreSQL/Testcontainers.
- Adapter/query phục vụ API Admin sau khi thống nhất port với Member 3.

Không viết Controller/JWT và không thay đổi quyết định nghiệp vụ của Member 1.

Nhánh đề xuất: `feature/persistence`.

### Member 3 — API / Admin / Security / Integration

Sở hữu:

- Controller và DTO của 8 operation.
- Mapping response/error/status/header đúng OpenAPI.
- `X-Event-Replayed` cho Gate API.
- Login, phát hành JWT role `ADMIN`, Gate API key.
- Admin query và maintenance application service.
- Validation HTTP, Swagger, Docker/README/CI và E2E.

Không đưa logic check-in vào Controller và không bypass Business Service để ghi database.

Nhánh đề xuất: `feature/api-security-admin`.

## 10. Quy trình Git

1. `basic_version` là nhánh nền dùng chung và phải luôn build/test được.
2. Không push trực tiếp lên `basic_version`; tạo PR.
3. PR đầu tiên là `chore/contracts-v1`, cập nhật domain/port/normalizer/OpenAPI và migration V3 đã thống nhất.
4. Chỉ tách ba nhánh feature sau khi contract PR được merge.
5. PR thay đổi port, schema hoặc HTTP contract phải có review của thành viên liên quan.
6. Mỗi PR ghi rõ thay đổi, contract bị tác động, lệnh test, migration và checklist pass/fail.
7. Không commit `.env`, mật khẩu, API key, JWT secret hoặc dữ liệu nhạy cảm.

## 11. Trình tự triển khai

| Mốc | Nội dung | Chủ trì | Điều kiện hoàn thành |
|---|---|---|---|
| M0 | Chốt contract, package, normalizer, port, ErrorResponse, Clock và migration V3. | Cả nhóm | `mvn clean verify` pass trên `basic_version`. |
| M1 | V3, adapter, locking, transaction và Testcontainers. | M2 | DB sạch migrate được; constraint có hiệu lực. |
| M2 | Check-in/checkout với mock port; controller/DTO/security làm song song. | M1 + M3 | Unit test Business pass; DTO đúng OpenAPI. |
| M3 | Ghép API -> Service -> PostgreSQL. | Cả nhóm | Một lượt ENTRY -> EXIT chạy đúng HTTP và DB. |
| M4 | Replay, concurrency, rollback, maintenance, bảo mật và paging. | Cả nhóm | Checklist mục 12 đạt. |
| M5 | Docker, README, Swagger, CI và demo máy khác. | M3 điều phối | Clone mới và chạy được theo README. |

## 12. Definition of Done

| ID | Kiểm thử bắt buộc | Chủ trì |
|---|---|---|
| T01 | ENTRY hợp lệ tạo đúng một session ACTIVE, spot OCCUPIED; trả 201. | M1 + M3 |
| T02 | EXIT hợp lệ chuyển session COMPLETED, spot AVAILABLE; trả 200. | M1 + M3 |
| T03 | ENTRY biển số đang ACTIVE trả 409; không tạo session thứ hai. | M1 |
| T04 | Hết chỗ đúng loại hoặc toàn bộ chỗ bảo trì trả `NO_AVAILABLE_SPOT`. | M1 |
| T05 | EXIT không có ACTIVE trả `NO_ACTIVE_SESSION`. | M1 |
| T06 | Retry cùng eventId/hash trả cùng body/status, header replay=true và không thay đổi DB thêm. | M1 + M2 + M3 |
| T07 | Cùng eventId nhưng hash khác trả `EVENT_ID_REUSED`. | M1 + M2 |
| T08 | Hai ENTRY tranh một spot: tối đa một request được cấp; DB không trùng. | M2 |
| T09 | Lỗi sau khi cập nhật spot nhưng trước khi tạo session rollback toàn bộ. | M2 |
| T10 | Spot bảo trì không được phân bổ; bật bảo trì spot OCCUPIED trả 409. | M2 + M3 |
| T11 | Maintenance đồng thời ENTRY không tạo trạng thái bất hợp lệ. | M2 + M3 |
| T12 | DELETE maintenance lần hai vẫn trả 204. | M3 |
| T13 | List spot/session lọc, sort và phân trang đúng; thời gian UTC. | M2 + M3 |
| T14 | Gate thiếu/sai key, Admin thiếu JWT và login sai đều trả 401 phù hợp. | M3 |
| T15 | `mvn clean verify`, Docker Compose, Flyway, Swagger UI và README được kiểm tra thực tế. | M3 |

Mức test tối thiểu:

- Member 1: unit test với repository port mock.
- Member 2: integration test PostgreSQL thật bằng Testcontainers; không thay bằng H2 cho locking/partial index.
- Member 3: controller/security test và ít nhất một E2E request qua API -> Service -> DB.

## 13. Checklist trước khi code

- [ ] Cả nhóm pull cùng nhánh `basic_version`.
- [ ] Đọc OpenAPI tại `src/main/resources/static/openapi.yaml`.
- [ ] Chốt contract PR trước khi mở nhánh feature dài hạn.
- [ ] Xác nhận migration tiếp theo là V3.
- [ ] Xác nhận normalizer và `licensePlateKey` dùng chung.
- [ ] Member 2 xác nhận transaction, locking và GateEvent concurrency.
- [ ] Member 3 xác nhận DTO, status, error code, JWT claim `roles` và replay header.
- [ ] Seed/Admin được cấu hình qua env; không lộ secret.
- [ ] Build và test baseline thành công.

## 14. Các file bàn giao trong repository

- `docs/TEAM_HANDOFF.md` — tài liệu này.
- `src/main/resources/static/openapi.yaml` — hợp đồng 8 API và nguồn Swagger UI.
- `README.md` — hướng dẫn chạy dự án.
- `.env.example` — mẫu cấu hình không chứa secret thật.
- `.github/workflows/ci.yml` — CI build và test.

Nhóm được xem là sẵn sàng code khi PR `chore/contracts-v1` được merge, cả ba thành viên compile được domain/port, hiểu rõ file mình sở hữu và chạy được test baseline.
