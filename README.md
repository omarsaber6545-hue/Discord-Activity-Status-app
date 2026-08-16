# 🎮 omar dev - Discord Activity Status (Android APK & CI/CD) 🚀

تطبيق أندرويد احترافي وحديث (**Android Native - Kotlin & Jetpack Compose**) لإدارة وتخصيص نشاط وحالة حسابك على ديسكورد مع محاكاة منصات الكونسول ونظارات الواقع الافتراضي (**VR / PS5 / Xbox / Mobile Spoofer**)، الإقامة بالروم الصوتي 24/7، الرد التلقائي عند الغياب، ونظام بناء آلي كامل عبر **GitHub Actions**.

> 👨‍💻 **المطور (Developer):** [**Omar-Dev (omarsaber6545-hue)**](https://github.com/omarsaber6545-hue) 🔥  
> 🏷️ **الإصدار الحالي (Version):** **v2.3.0**  
> 📱 **المنصة (Platform):** Android 7.0+ (API 24 -> 34)

---

## 🌟 المميزات الرئيسية (Key Features)

- 🎨 **تصميم داكن فاخر (Dark Premium AMOLED UI)**: واجهة مستخدم سريعة وسلسة مبنية بأحدث معايير **Jetpack Compose & Material 3**.
- 🪟 **معاينة حية فورية (Live Discord Preview Card)**: كارت بروفايل تفاعلي يتحدث لحظياً مع الكتابة يظهر الأفاتار، اسم اللعبة، التفاصيل، الصور، وعداد الوقت.
- ⚡ **أوضاع ألعاب سريعة (Quick Presets)**: تبديل فوري بلمسة واحدة (`VS Code`, `Valorant`, `Minecraft`, `GTA V`, `CS2`, `League of Legends`, `Roblox`, `AFK`).
- 🥽 **محاكي المنصات والـ VR (Device & Console Spoofer)**:
  - 🥽 نظارات Meta Quest 3 (VR).
  - 🎮 أجهزة PlayStation 5.
  - 🟩 أجهزة Xbox Series X.
  - 📱 أجهزة الهاتف المحمول (Android).
- 🎙️ **البقاء بالروم الصوتي 24/7 (Voice Channel Stay)**: اتصال مباشر بـ Discord Gateway مع دعم `Self Mute` و `Self Deaf`.
- 🤖 **نظام الرد التلقائي عند الغياب (AFK Auto-Responder)**: رد تلقائي على الرسائل الخاصة والمنشن مع كولداون ذكي لحماية الحساب.
- 🔔 **مركز التنبيهات وسجل الأحداث (Notification Center)**: شاشة سجلات متكاملة للأحداث، الأخطاء، والعمليات الناجحة.
- 🛡️ **التوافق التام مع سياسات ديسكورد (100% Discord ToS Compliant)**: يعتمد على بروتوكول Discord Bot API & Gateway الرسمي دون أي أدوات مخالفة أو Selfbots.

---

## 📂 هيكل المشروع (Project Structure)

```text
Discord-Activity-Status/
│
├── app/                                 # كود وموارد تطبيق الأندرويد
│   ├── src/main/
│   │   ├── AndroidManifest.xml          # الصلاحيات وشاشة البداية
│   │   ├── java/com/omardev/discordactivity/
│   │   │   ├── MainActivity.kt          # النشاط الرئيسي
│   │   │   ├── App.kt                   # كلاس التطبيق وقنوات الإشعارات
│   │   │   ├── data/                    # نماذج البيانات وإدارة الإعدادات
│   │   │   ├── network/                 # محرك اتصال WebSocket بـ Discord Gateway
│   │   │   ├── service/                 # خدمة الخلفية المستقرة (Foreground Service)
│   │   │   └── ui/                      # الواجهات والمكونات والثيم الداكن
│   │   └── res/                         # الصور، الأيقونات، الألوان، والقيم
│   ├── build.gradle.kts                 # إعدادات موديول التطبيق والتبعيات
│   └── proguard-rules.pro               # قواعد الحماية والضغط
│
├── .github/
│   └── workflows/
│       └── build-apk.yml                # خط سير العمل للبناء التلقائي للـ APK
│
├── gradle/wrapper/                      # ملفات Gradle Wrapper
├── build.gradle.kts                     # إعدادات المشروع العامة
├── settings.gradle.kts                  # إعدادات الموديولات
├── gradlew                              # مشغل Gradle لنظام Linux / macOS
├── gradlew.bat                          # مشغل Gradle لنظام Windows
└── README.md                            # التوثيق ودليل الاستخدام
```

---

## 🚀 طريقة البناء والتحميل عبر GitHub Actions (CI/CD)

تم ضبط ملف `.github/workflows/build-apk.yml` بحيث يقوم ببناء ملف الـ APK وتوقيعه تلقائياً فور رفع الكود:

### 1️⃣ تحميل الـ APK من الـ Artifacts:
1. ارفع الكود إلى مستودع GitHub الخاص بك.
2. اذهب إلى تبويب **Actions** في صفحة المستودع.
3. اضغط على أحدث Workflow مكتمل باسم **Build & Release Android APK**.
4. في أسفل الصفحة ستجد قسم **Artifacts**، اضغط على **`Discord-Activity-Status-APK`** لتحميل الـ APK وتثبيته مباشرة على هاتفك!

### 2️⃣ إنشاء Release تلقائي عند إصدار نسخة جديدة:
قم بإنشاء Tag في الـ Git ودفعه إلى GitHub:
```bash
git tag v2.3.0
git push origin v2.3.0
```
> سيقوم GitHub Actions ببناء التطبيق ونشر Release رسمي في صفحة **Releases** يحتوي على ملف الـ APK الجاهز مباشرة!

---

## 💻 طريقة البناء والتشغيل محلياً (Local Build)

### 📌 المتطلبات المحلية:
- **JDK 17** (Java Development Kit).
- **Android SDK** (API 34).

### 🛠️ أوامر البناء:

#### على نظام Windows:
```cmd
.\gradlew.bat assembleRelease
```
أو لبناء نسخة الـ Debug السريعة:
```cmd
.\gradlew.bat assembleDebug
```

#### على نظام Linux / macOS:
```bash
chmod +x gradlew
./gradlew assembleRelease
```

> تجد ملف الـ APK الناتج داخل المسار:  
> `app/build/outputs/apk/release/app-release.apk` أو `app/build/outputs/apk/debug/app-debug.apk`

---

## 🔐 إعداد المفاتيح والـ Secrets (Security Best Practices)

لا تضع أي بيانات حساسة داخل الكود المصدري. يمكنك ضبط الـ Secrets في حسابك على GitHub بالذهاب إلى:  
`Settings` -> `Secrets and variables` -> `Actions` -> `New repository secret`

| اسم السر (Secret Name) | الوصف |
| :--- | :--- |
| `DISCORD_TOKEN` | توكن البوت الرسمي من [Discord Developer Portal](https://discord.com/developers/applications) |
| `CLIENT_ID` | معرّف تطبيق ديسكورد (Application ID) |
| `CLIENT_SECRET` | المعرّف السري للتطبيق (OAuth2 Client Secret) |

---

## ⚙️ كيفية إعداد بوت ديسكورد الرسمي (How to Setup Discord Bot)

1. ادخل إلى [Discord Developer Portal](https://discord.com/developers/applications).
2. اضغط على **New Application** وقم بتسميته (مثلاً: `omar dev`).
3. من القائمة الجانبية ادخل إلى **Bot** ثم اضغط **Add Bot**.
4. فعّل خيارات الـ Intents الثلاثة (**Presence Intent**, **Server Members Intent**, **Message Content Intent**).
5. اضغط **Reset Token** وانسخ التوكن الناتج.
6. افتح التطبيق في هاتفك وألصق التوكن في خانة **Discord Bot Token** واضغط **START DISCORD PRESENCE**! 🚀

---

### 🔥 تم التطوير بواسطة: [**Omar-Dev (omarsaber6545-hue)**](https://github.com/omarsaber6545-hue) 🔥
