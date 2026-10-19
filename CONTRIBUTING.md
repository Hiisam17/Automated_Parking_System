# Hướng dẫn đóng góp

## Quy trình làm việc

1. Đồng bộ nhánh gốc trước khi bắt đầu.
2. Tạo nhánh mới theo một trong các tiền tố `feature/`, `fix/` hoặc `chore/`.
3. Viết commit ngắn gọn, mô tả đúng thay đổi; nên dùng dạng `type: mô tả`.
4. Chạy `mvn clean verify` trước khi mở Pull Request.
5. Push nhánh và tạo Pull Request để thành viên khác review.

Không push trực tiếp vào `main` hoặc nhánh mặc định khi làm việc nhóm. Mỗi Pull Request nên tập trung vào một mục tiêu và không trộn thay đổi không liên quan.
