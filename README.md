# EnglishTalk AI 🗣️

Ứng dụng luyện **nói tiếng Anh giọng bản xứ** với gia sư AI, chạy trên **Android và iOS** từ một codebase
**Kotlin Multiplatform + Compose Multiplatform**. AI dùng **Claude** (Anthropic).

## 1. Ứng dụng tương tự trên thị trường & tính năng được tham khảo

| Ứng dụng | Điểm mạnh nổi bật | Tính năng tương ứng trong EnglishTalk |
|---|---|---|
| **ELSA Speak** | Chấm điểm phát âm từng từ/âm, bài luyện theo âm khó, giọng Mỹ | Luyện phát âm theo âm (/θ/ /ð/, phụ âm cuối, r/l, v/w, s/ʃ, nguyên âm dài–ngắn, nối âm), tô màu từng từ, điểm 0–100, AI phân tích lỗi + IPA + cách đặt lưỡi/môi |
| **Speak** | Hội thoại nói với AI, gợi ý câu trả lời khi bí | Hội thoại bằng giọng nói, chip **gợi ý câu trả lời** (chạm để nghe) |
| **Praktika / Loora / TalkPal** | Gia sư AI nhập vai, phản hồi ngữ pháp sau mỗi câu | 10 tình huống nhập vai (cà phê, sân bay, khách sạn, bác sĩ, phỏng vấn, họp nhóm, tranh luận…) + trò chuyện tự do; sau mỗi câu có **sửa lỗi**, giải thích bằng tiếng Việt và **"người bản xứ nói thế này"** |
| **Duolingo (Max)** | Streak, XP, mục tiêu ngày, roleplay | Chuỗi ngày 🔥, XP, mục tiêu ngày, thống kê tiến độ, **tổng kết buổi học** có điểm & cụm từ nên học |
| **Cake** | Shadowing câu giao tiếp thực tế | Nghe giọng bản xứ ở tốc độ thường/chậm 🐢 rồi nhại lại, bài "Câu giao tiếp hằng ngày" |
| **Flashcard (Anki, Quizlet)** | Ôn từ vựng ngắt quãng | Flashcard lật thẻ, phát âm, nói thử để chấm điểm, hàng đợi "Học lại"; **AI tạo bộ từ theo chủ đề** bạn nhập |

Các tính năng chung khác: chọn **giọng Mỹ / Anh / Úc**, chỉnh tốc độ đọc, trình độ CEFR A1–C2 (AI điều chỉnh
độ khó), bản dịch tiếng Việt, tự động đọc câu trả lời.

## 2. Kiến trúc

```
composeApp/
  src/commonMain/   ← ~90% code dùng chung (UI Compose, AI, chấm điểm, lưu trữ)
    ai/             ClaudeClient (REST qua Ktor), TutorService (prompt + JSON schema)
    data/           Nội dung bài học (Catalog), AppRepository (cài đặt, XP, streak, bộ từ AI)
    scoring/        PronunciationScorer – căn chỉnh từ (edit distance) giữa câu mẫu và câu nhận dạng
    platform/       interface SpeechRecognizer / TextToSpeech (expect/actual)
    ui/             Các màn hình Compose: home, conversation, pronunciation, vocabulary, settings
  src/androidMain/  android.speech.SpeechRecognizer + android.speech.tts.TextToSpeech, MainActivity
  src/iosMain/      SFSpeechRecognizer + AVAudioEngine, AVSpeechSynthesizer, MainViewController
  src/desktopMain/  Bản desktop để xem nhanh UI (không có mic/TTS)
iosApp/             App SwiftUI nhúng Compose (tạo project Xcode bằng XcodeGen)
```

- **Giọng nói → chữ (STT)** và **chữ → giọng nói (TTS)** dùng engine native của hệ điều hành (miễn phí,
  độ trễ thấp). iOS tự chọn giọng *Enhanced/Premium* nếu đã tải (Cài đặt › Trợ năng › Nội dung được đọc).
  Android chọn giọng chất lượng cao nhất đã cài.
- **AI** gọi Claude Messages API trực tiếp bằng Ktor (không có Anthropic SDK chính thức cho Kotlin/Native).
  Mọi phản hồi dùng **structured outputs** (`output_config.format` + JSON schema) để UI hiển thị phản hồi,
  sửa lỗi, gợi ý một cách tin cậy. Mặc định model `claude-opus-5-5`, có thể đổi sang Sonnet 5.5 / Haiku 5.5
  (nhanh và rẻ hơn) trong Cài đặt. Với Opus/Sonnet 5.5 app bật `fallbacks: "default"` để yêu cầu bị từ chối
  nhầm sẽ được chạy lại trên model dự phòng.
- **Chấm phát âm**: so khớp từng từ giữa câu mẫu và kết quả nhận dạng (đúng / gần đúng / sai / thiếu), sau đó
  Claude phân tích lỗi điển hình của người Việt (âm cuối, th, r/l…) và hướng dẫn cách sửa. Muốn chấm ở mức
  âm vị (phoneme) như ELSA, có thể cắm thêm dịch vụ chuyên dụng (vd. Azure Pronunciation Assessment) vào
  `PronunciationScorer`.

## 3. Chạy ứng dụng

Yêu cầu: JDK 17+, Android Studio (Ladybug trở lên) với Android SDK 36; Xcode 16+ để chạy iOS.

**Android**
```bash
./gradlew :composeApp:installDebug      # hoặc mở thư mục dự án bằng Android Studio và bấm Run
```

**iOS**
```bash
brew install xcodegen
cd iosApp && xcodegen generate && open iosApp.xcodeproj
```
Chọn Team ký ứng dụng (hoặc điền `TEAM_ID` trong `iosApp/Configuration/Config.xcconfig`) rồi Run.
Bước build Xcode tự gọi `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`.
Lưu ý: nhận dạng giọng nói cần chạy trên máy thật để có micro.

**Desktop (xem nhanh UI)**
```bash
./gradlew :composeApp:run
```

**Kiểm thử**
```bash
./gradlew :composeApp:desktopTest
```

## 4. Cấu hình AI

1. Tạo API key tại <https://console.anthropic.com>.
2. Mở app › **Cài đặt** › dán key vào ô *Claude API key*.

> ⚠️ Key được lưu cục bộ trên thiết bị — phù hợp để phát triển/dùng cá nhân. Khi phát hành lên store, **không
> nhúng key vào app**: hãy dựng một proxy nhỏ trên server giữ key (thêm xác thực người dùng, giới hạn tần suất)
> và nhập địa chỉ proxy vào ô *API base URL*. Proxy chỉ cần chuyển tiếp `POST /v1/messages`.

## 5. Hướng phát triển tiếp

- Chấm điểm ở mức âm vị, biểu đồ ngữ điệu (intonation).
- Streaming câu trả lời và TTS theo câu để giảm độ trễ.
- Đồng bộ tiến độ lên cloud, đăng nhập, bảng xếp hạng.
- Thông báo nhắc học hằng ngày, lặp lại ngắt quãng (SRS) cho từ vựng.
