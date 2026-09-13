# اشتراكاتي | Ishtirakati
تطبيق Android أصلي بـ Kotlin وJetpack Compose لمشتركي Skyline SAS Radius v4.

## ما تم تنفيذه
- تسجيل الدخول إلى SAS API مع AES-256-CBC OpenSSL Salted format مطابق للمرجع.
- Retrofit/OkHttp/Gson، حفظ آمن للتوكن وبيانات الدخول عبر EncryptedSharedPreferences.
- لوحة عربية RTL تعرض بيانات الاشتراك وتتعامل مع أسماء الحقول البديلة.
- WorkManager كل 15 دقيقة لتنبيهات قبل 24 ساعة وقبل ساعة وعند الانتهاء مع منع التكرار.
- Material 3، طلب POST_NOTIFICATIONS لأندرويد 13+، تحديث وتسجيل خروج.

## البناء
```bash
./gradlew assembleDebug
./gradlew assembleRelease
```
ملحوظة: الخادم الحالي يستخدم HTTP كما ورد في المواصفات، لذلك تم تفعيل cleartext traffic. يفضّل استخدام HTTPS عند توفره.

## Release

- Version: v1.0.0
- Signed APK: `release/ishtirakati-v1.0.0.apk`
- SHA-256: `4081f2dfd0826657b6ab7763dcaaa5dc5fbb9074e36f1b69d1cd368f7cdaee84`

The APK is built for Android API 24+ and signed with APK Signature Scheme v2/v3.
