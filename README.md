# QBaoTweakxVft Android source

## Mở project
1. Giải nén ZIP.
2. Mở thư mục `QBaoTweakxVft_Android` bằng Android Studio.
3. Cài Android SDK Platform 35 và dùng JDK 17.
4. Chờ Gradle Sync rồi chọn Run để cài lên thiết bị hoặc Build > Build APK(s).

## Key API
Ứng dụng gọi `POST https://qbaonew1.onrender.com/api/check-key` với JSON `{"key":"..."}`. Backend cần trả `valid`, `message`, và `expiresAt` như server trong ZIP `Qbaonew_PostgreSQL_Render.zip`.

## Giao diện\nGiao diện sử dụng tông tím-đỏ đơn sắc; không còn banner hoặc liên kết ảnh anime trong ba mục.\n\n## Video
Video được chép vào `app/src/main/res/raw/login_video.mp4`; nếu file có âm thanh, màn hình nhập key phát âm thanh và lặp video.

## Lưu ý
- ZIP backend được cung cấp có endpoint kiểm tra key nhưng không có API key bí mật dành cho ứng dụng; app gọi endpoint công khai của server.
- Đây là project mã nguồn, không phải APK đã biên dịch. Cần Android Studio/SDK để build APK.
- Các nút aim là công tắc giao diện, không can thiệp bộ nhớ hoặc thay đổi cơ chế game. Mục tối ưu chỉ đưa ra hướng dẫn an toàn, không thể cam kết tối ưu 100%.
