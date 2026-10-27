# Bàn giao Member 2: PostgreSQL persistence và transaction

## Phạm vi

Triển khai trên nhánh `feature/persistence`, từ `basic_version` tại `ebd32aa`.
Chỉ thay đổi infrastructure, migration, integration test và tài liệu bàn giao này.
Giữ nguyên toàn bộ source và unit test Business của Member 1, API, DTO,
OpenAPI, security/JWT của Member 3, và lịch sử migration V1/V2.

## Wiring và adapter

`ParkingWorkflowConfiguration` cung cấp bean `ParkingService` và `Clock.systemUTC()`.
Bean `PersistentParkingService` gọi nguyên trạng `CheckInService` / `CheckOutService`
của Member 1, kết nối các port với PostgreSQL. Member 3 có thể inject
`ParkingService` vào controller khi tích hợp Gate API.

Port `GateEventWorkflowRepository.beginOrReplay(eventId, eventType, requestHash)`
hiện tại không có `gateId`, trong khi cột `gate_events.gate_id` là bắt buộc.
Infrastructure gắn `gateId` từ command vào một implementation của port dành riêng
cho mỗi lần gọi, rồi chuyển đến `GateEventWorkflowRepositoryAdapter`.
Không sửa chữ ký port, không dùng gate ID giả hoặc ThreadLocal. Các request đồng thời
có implementation riêng nên không chia sẻ metadata của nhau.

Workflow adapter dùng `JdbcTemplate` cho SQL PostgreSQL tường minh. CRUD adapter
JPA vẫn được giữ; `ParkingSessionRepositoryAdapter` có transaction bao phủ việc
mapping quan hệ lazy khi `open-in-view=false`.

## Transaction và locking

- `SpringParkingUnitOfWork` dùng transaction manager do Spring Boot cấu hình,
  propagation `REQUIRED`, isolation `READ_COMMITTED`. JPA và JDBC cùng DataSource
  tham gia cùng transaction. Từ chối tham gia outer transaction read-only hoặc
  isolation tường minh khác `READ_COMMITTED`.
- Các phương thức workflow đọc khóa/ghi dữ liệu có propagation `MANDATORY`.
  Không gọi trực tiếp ngoài `ParkingUnitOfWork`.
- Thứ tự xử lý trong workflow: event → khóa biển số → khóa spot/session → ghi kết quả.
- ENTRY và EXIT cùng lấy `pg_advisory_xact_lock(hashtextextended(...))` theo
  `licensePlateKey`. Khóa này bảo vệ cả trường hợp chưa có dòng session để khóa,
  đồng thời được chia sẻ giữa các instance backend và tự nhả khi transaction kết thúc.
- Phân chỗ dùng `ORDER BY code ASC LIMIT 1 FOR UPDATE`, lọc loại xe,
  `AVAILABLE` và `in_maintenance=false`. Chờ khóa thay vì bỏ qua chỗ đang bị khóa;
  tránh lưu từ chối hết chỗ chỉ vì transaction khác chưa hoàn tất.
- Checkout khóa session ACTIVE bằng `FOR UPDATE OF s` và cập nhật có điều kiện.
  `markOccupied`, `markAvailable`, `complete` chỉ trả `true` khi cập nhật đúng một
  dòng ở trạng thái mong đợi. Business có sẵn sẽ ném lỗi nếu cập nhật bất thường.
- Event mới dùng `INSERT ... ON CONFLICT (event_id) DO NOTHING`. Request trùng ID
  chờ transaction sở hữu event hoàn tất, sau đó đọc kết quả bằng một statement mới
  `SELECT ... FOR UPDATE`. Cùng payload trả lại `GateDecision` từ JSONB; khác gate,
  loại event hoặc hash trả `Reused`. Không bắt unique violation để tiếp tục một
  transaction PostgreSQL đã lỗi.
- Quyết định chấp nhận và từ chối nghiệp vụ đều được lưu. Lỗi kỹ thuật rollback
  GateEvent, ParkingSpot và ParkingSession; retry cùng ID có thể chạy lại.
  Event đã hoàn tất không được ghi đè. Event PROCESSING tồn đọng hoặc JSONB hỏng
  gây lỗi kỹ thuật, không tạo quyết định mới từ dữ liệu không hợp lệ.

Các quy tắc SQL dựa trên [PostgreSQL 17: transaction isolation](https://www.postgresql.org/docs/17/transaction-iso.html)
và [PostgreSQL 17: explicit locking](https://www.postgresql.org/docs/17/explicit-locking.html).

## Migration V3 và constraint

`V3__add_license_plate_key.sql`:

1. Thêm cột `license_plate_key`, backfill mọi session bằng uppercase và loại bỏ
   hyphen / khoảng trắng theo đúng normalizer hiện có, kể cả Unicode separator.
2. Đặt `NOT NULL`; kiểm tra key gồm 4–20 chữ/số uppercase và khớp với biển số gốc.
3. Tạo unique index ACTIVE trên key và bỏ unique index cũ trên biển số hiển thị.
4. Giữ unique index ACTIVE theo spot, foreign key, constraint timeline và completion.
5. Thêm index `(license_plate_key, check_in_time DESC, id DESC)` cho tra cứu lịch sử.

JPA entity lưu key; mapper tính key bằng `LicensePlateNormalizer` khi ghi qua CRUD.
Domain record không cần thay đổi vì workflow port đã truyền key riêng.

Nếu dữ liệu cũ có nhiều session ACTIVE quy về cùng key hoặc key không hợp lệ,
V3 sẽ thất bại và rollback nguyên tử. Không xóa, tự hoàn tất hoặc sửa biển số cũ.
Người vận hành cần đối chiếu và xử lý dữ liệu hợp lệ trước khi chạy lại migration.
Không sửa V1/V2 và không xóa lịch sử Flyway để bỏ qua lỗi.

## Điểm tích hợp bảo trì cho Member 3

Integration test kiểm tra ENTRY cạnh tranh với conditional SQL cập nhật bảo trì
trên cùng dòng `parking_spots`:

```sql
UPDATE parking_spots
SET in_maintenance = TRUE
WHERE id = :spotId
  AND occupancy_status = 'AVAILABLE'
  AND NOT in_maintenance;
```

Khi triển khai maintenance application service/port, Member 3 cần dùng conditional
update này hoặc khóa dòng và kiểm tra lại trạng thái trong cùng transaction.
Không dùng CRUD `findById` rồi `save` một snapshot cũ để bật bảo trì. Test này xác
minh cơ chế cạnh tranh ở database; controller, mã lỗi HTTP, Admin query/paging và
maintenance application service vẫn thuộc phần tích hợp Member 3.

## Chạy kiểm thử và demo phần persistence

Yêu cầu: JDK 21+, Maven và Docker Engine đang chạy, tải được image `postgres:17-alpine`.
Không cần `.env`, PostgreSQL cài sẵn hoặc database dự án để chạy test;
Testcontainers tạo database tạm và tự dọn container.

```powershell
docker info
mvn --batch-mode clean verify
```

Chạy riêng các integration test Member 2:

```powershell
mvn --batch-mode '-Dit.test=ParkingPersistenceIT,LicensePlateKeyMigrationIT' verify
```

Demo một lượt ENTRY → EXIT từ Business Service xuống PostgreSQL thật:

```powershell
mvn --batch-mode '-Dit.test=ParkingPersistenceIT#entryAndExitCommitSessionSpotAndAuditWithUtcTimes' verify
```

Integration test mặc định tự skip khi không có Docker, theo cấu hình baseline.
Cần kiểm tra báo cáo để chắc chắn `Skipped: 0` nếu muốn xác minh locking/constraint.
Báo cáo: `target/surefire-reports` (unit), `target/failsafe-reports` (integration).

Các ca Member 2 bao phủ:

- ENTRY/EXIT commit đúng spot, session, audit; biển số chuẩn hóa và thời gian UTC.
- Mapping JPA với lazy relation, key đồng nhất giữa workflow và CRUD.
- Replay kết quả chấp nhận/từ chối sau khi trạng thái bãi đổi; ID dùng chung ENTRY/EXIT.
- Rollback sau khi đổi spot, tạo session, ghi audit hoặc giải phóng spot gặp lỗi.
- Lỗi unique constraint SQL thật rollback cả các thay đổi JPA lẫn JDBC.
- Thiếu transaction, outer isolation/read-only không phù hợp, conditional update sai trạng thái.
- Unique ACTIVE theo key/spot, NOT NULL, key khớp biển số, foreign key, timeline/completion.
- Hai ENTRY tranh một spot, hai biển số tranh phân chỗ, cùng biển số khác cách viết,
  hai EXIT đồng thời, và ENTRY cạnh tranh với conditional maintenance update.
- Hai request cùng event ID chờ khóa PostgreSQL thật: chủ sở hữu commit thì replay,
  chủ sở hữu rollback thì request chờ tiếp tục xử lý event mới.
- Flyway V2 → V3 trên database sạch, lịch sử có Unicode whitespace, dữ liệu ACTIVE
  xung đột và biển số không hợp lệ; xác minh không mất lịch sử khi migrate thất bại.

`ApplicationIntegrationIT` vẫn khởi động backend và kiểm tra health, OpenAPI,
Swagger UI; cập nhật duy nhất ở test này là version migration hiện tại từ 2 lên 3.
Luồng Gate HTTP cần được Member 3 kết nối với bean `ParkingService` trước khi demo qua REST.

## Kết quả xác minh ngày 25/10/2026

- `mvn --batch-mode clean verify`: **BUILD SUCCESS**; 30 unit test và 35 integration
  test, không lỗi, không thất bại, không skip. Trong đó có 32 integration test mới
  của Member 2 và 3 integration test khởi động ứng dụng sẵn có.
- Môi trường thực thi: JDK Corretto 22.0.2 (compile `--release 21`), Maven 3.8.5
  có sẵn trên máy, Docker Engine 27.5.1, PostgreSQL `17-alpine` qua Testcontainers.
- Chạy file JAR đã đóng gói với một PostgreSQL container tạm riêng: backend khởi động,
  Flyway đạt V3, seed đủ 30 spot, `/actuator/health` trả 200/UP,
  `/openapi.yaml` và `/swagger-ui/index.html` trả 200.
- Luồng ENTRY → EXIT được chạy qua Business Service → adapter → PostgreSQL thật
  trong integration test. Không báo cáo đây là E2E Gate HTTP.
- Đã dừng ứng dụng và dọn PostgreSQL container tạm sau khi chạy thử.

## Đối chiếu TEAM_HANDOFF trước khi push

| Tiêu chí thuộc Member 2 | Bằng chứng trong implementation / integration test |
|---|---|
| §7.2, D07, D10: khóa, cập nhật có điều kiện và transaction | Workflow adapter `MANDATORY`, `ParkingUnitOfWork` READ_COMMITTED, phân chỗ theo code; kiểm tra trạng thái mong đợi và rollback cả JPA/JDBC. |
| §7.3, T06–T07: replay và event ID toàn hệ thống | Upsert + khóa PostgreSQL; replay cả chấp nhận/từ chối; từ chối payload/gate/type khác; kiểm thử request cùng ID chờ commit hoặc rollback. |
| §8, D08: migration và constraint | V3 backfill key, NOT NULL, partial unique index theo key/spot, index lịch sử; kiểm thử V2 → V3 và rollback migration khi dữ liệu cũ xung đột. V1/V2 không đổi. |
| D12: thời gian backend qua Clock, UTC | Session giữ Clock của Business; `gate_events.created_at` được adapter ghi từ Clock, thay vì phụ thuộc giá trị mặc định của database. Test xác nhận audit bằng thời gian cố định. |
| T08–T09: cạnh tranh chỗ và rollback | Hai ENTRY tranh một spot chỉ một request thành công; lỗi trước khi tạo session hoặc khi lưu audit rollback toàn bộ và cho retry cùng event ID. |
| T10–T11, phần database | Không cấp spot bảo trì; conditional maintenance update tranh ENTRY không tạo trạng thái vừa OCCUPIED vừa bảo trì. |
| §9, ranh giới Member 2 | Không sửa Business/port/unit test Member 1, Controller/DTO/JWT/OpenAPI Member 3. Binding gateId và wiring chỉ nằm trong infrastructure. |
| §12, mức test tối thiểu | Kiểm tra trên PostgreSQL thật bằng Testcontainers, không dùng H2 cho locking hoặc partial index. |

Các phần HTTP của T06/T10, maintenance application service, Admin paging và E2E
API tiếp tục cần Member 3 tích hợp theo contract; bảng trên không đánh dấu các
phần đó đã hoàn tất. Nhánh feature được push để review; chưa merge schema vào
`basic_version`.

Kết quả chạy lại sau khi sửa audit time: `mvn --batch-mode clean verify` trên
image `maven:3.9.16-eclipse-temurin-21-alpine` (Java 21.0.12.1), dùng PostgreSQL
17.11 qua Testcontainers: **BUILD SUCCESS**, 30 unit test + 35 integration test,
không failure/error/skip. Assertion mới xác minh `gate_events.created_at` khớp
Clock cố định. Phần persistence của Member 2 đạt các tiêu chí đối chiếu ở trên.
