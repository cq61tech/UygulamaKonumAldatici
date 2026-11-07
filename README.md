# Konum Aldatıcı / Location Spoofer

[Türkçe](#türkçe) · [English](#english)

---

## Türkçe

Root ve LSPosed ile çalışan bir Android modülü. Seçtiğin uygulamalara, telefonun tamamını değiştirmeden, başka bir ülkede görünme imkânı verir. Sistem genelinde bir değişiklik yapmaz; ayarlar yalnızca seçilen uygulamalar için geçerlidir.

### Ne yapar

- **Konum:** `Location` nesnelerinin koordinatlarını seçilen ülkeden döndürür, sahte konum bayrağını gizler.
- **SIM ve operatör:** ülke kodu, operatör adı ve MCC/MNC değerlerini değiştirir.
- **Saat dilimi** ve **bölge/dil:** isteğe bağlı katmanlar.
- **VPN (WireGuard):** yalnızca seçilen uygulamaların trafiğini tünele alır. Diğer uygulamalar normal bağlantıyla devam eder.
- **VPN gizleme:** uygulamanın ağın VPN olduğunu öğrenmesini engeller.
- Ülke seçimi hem varsayılan hem uygulama bazlı yapılabilir. Hazır ülke profilleri: ABD, Birleşik Krallık, Almanya, Fransa, Hollanda, Kanada, Japonya, Güney Kore, Singapur, İsveç, İsviçre, İspanya, İtalya, Polonya, BAE, Avustralya, Brezilya, Hindistan, Norveç, Romanya, Meksika.

### Gereksinimler

- Android 10 – 17
- Root (Magisk, KernelSU, APatch)
- Zygisk destekli bir LSPosed sürümü (libxposed API 101 veya üstü)

### Kurulum

1. [Releases](../../releases) sayfasından APK'yı indirip kur.
2. LSPosed → Modüller → **Konum Aldatıcı**'yı etkinleştir.
3. Uygulamayı kapatıp yeniden aç. Ana ekran "LSPosed'a bağlı" göstermelidir.

### Kullanım

1. **Uygulamalar** sekmesinde uygulamaları seç, **Kaydet**'e bas. LSPosed kapsam isteğini onayla.
2. **VPN** sekmesinde hedef ülkeyi seç.
3. VPN için: **Profilleri içe aktar** ile WireGuard `.conf` dosyalarını ekle (örneğin Proton VPN'den indirdiğin dosyalar), sonra **Bağlan**'a bas.
4. Spoof edilen uygulamayı zorla durdurup yeniden başlat.

Katman ayarları (**Ayarlar → Spoof katmanları**) ilgili uygulama yeniden başlatılınca geçerli olur.

### Sınırlamalar

- IP adresi yalnızca VPN ile değişir. VPN kapalıyken uygulamalar gerçek IP'ni görür.
- Android aynı anda tek VPN'e izin verir; VPN tek ülkede çalışır.
- Bankacılık uygulamaları, Play Integrity ve sunucu tarafı doğrulamalar desteklenmez.
- Kök erişimini veya hook'ları algılayan uygulamalar (örneğin TikTok) açılışta takılabilir.
- Canlı konum akışı gerçek GPS'ten gelir; sentetik konum yalnızca "son bilinen konum" için üretilir.
- Google Play ve Google Play Hizmetleri listede gösterilmez ve değiştirilmez.

### Gizlilik

- Analitik, izleme ya da reklam yoktur.
- Ağ erişimi yalnızca WireGuard tüneli içindir; ek olarak **IP'yi kontrol et** düğmesine basınca `ipwho.is` adresine tek bir istek gider.
- WireGuard profilleri uygulamanın özel klasöründe tutulur. **Özel anahtar içeren `.conf` dosyalarını depoya ya da herkese açık bir yere yükleme.** `.gitignore` bu dosyaları dışarıda tutar.

### Kaynaktan derleme

Derleme GitHub Actions üzerinde yapılır (`.github/workflows/build.yml`). Yerelde: JDK 17 ve Gradle 8.11.1 ile `gradle assembleDebug`.

Sabit imzalı sürüm için depo gizli değişkenleri (Settings → Secrets and variables → Actions):

| Ad | Açıklama |
|----|----------|
| `KEYSTORE_BASE64` | `.jks` dosyasının base64 hâli |
| `KEYSTORE_PASSWORD` | anahtar deposu şifresi |
| `KEY_ALIAS` | anahtar takma adı |
| `KEY_PASSWORD` | anahtar şifresi |

`v0.4.1` gibi bir etiket gönderildiğinde imzalı APK otomatik olarak Release'e eklenir.

### Lisans ve kullanılan projeler

[Apache License 2.0](LICENSE). Kullanılan açık kaynak projeler: [libxposed](https://github.com/libxposed) (Apache-2.0), [wireguard-android](https://git.zx2c4.com/wireguard-android) tünel kütüphanesi (Apache-2.0), AndroidX ve Jetpack Compose (Apache-2.0).

### Sorumluluk reddi

Bu proje gizlilik ve test amaçlıdır. Kullanım sırasında yürürlükteki yasalara ve ilgili hizmetlerin kullanım şartlarına uymak kullanıcının sorumluluğundadır. Proje; Proton, TikTok, Telegram, WhatsApp, Instagram, YouTube veya LSPosed ile bağlantılı değildir. Yazılım "olduğu gibi" sunulur.

---

## English

An Android module for rooted devices running LSPosed. It lets the apps you choose appear to be in another country without changing anything system-wide; settings apply only to the selected apps.

### Features

- **Location:** returns coordinates for the chosen country from `Location` objects and hides the mock-provider flag.
- **SIM and carrier:** changes the country code, carrier name and MCC/MNC.
- **Time zone** and **region/language:** optional layers.
- **VPN (WireGuard):** routes only the selected apps through the tunnel; everything else keeps using the normal connection.
- **Hide VPN:** stops the app from learning that the network is a VPN.
- Country can be set globally or per app. Built-in profiles: United States, United Kingdom, Germany, France, Netherlands, Canada, Japan, South Korea, Singapore, Sweden, Switzerland, Spain, Italy, Poland, UAE, Australia, Brazil, India, Norway, Romania, Mexico.

### Requirements

- Android 10 – 17
- Root (Magisk, KernelSU, APatch)
- An LSPosed build with Zygisk support (libxposed API 101 or newer)

### Install

1. Download the APK from the [Releases](../../releases) page and install it.
2. LSPosed → Modules → enable **Location Spoofer**.
3. Close and reopen the app. The home screen should show "Connected to LSPosed".

### Usage

1. In the **Apps** tab, pick apps and press **Save**. Approve the LSPosed scope request.
2. In the **VPN** tab, choose the target country.
3. For the VPN: use **Import profiles** to add WireGuard `.conf` files (for example ones downloaded from Proton VPN), then press **Connect**.
4. Force-stop and relaunch the spoofed app.

Layer changes (**Settings → Spoofing layers**) take effect when the app is relaunched.

### Limitations

- The IP address only changes through the VPN. With the VPN off, apps see your real IP.
- Android allows one VPN at a time, so the VPN runs in a single country.
- Banking apps, Play Integrity and server-side checks are not supported.
- Apps that detect root or hooks (TikTok, for example) may hang at launch.
- Live location updates come from the real GPS; a synthetic location is only produced for "last known location".
- Google Play and Google Play services are hidden from the list and never modified.

### Privacy

- No analytics, tracking or ads.
- Network access is limited to the WireGuard tunnel; pressing **Check IP** additionally sends a single request to `ipwho.is`.
- WireGuard profiles are stored in the app's private storage. **Never publish `.conf` files; they contain private keys.** `.gitignore` keeps them out of the repository.

### Building

Builds run on GitHub Actions (`.github/workflows/build.yml`). Locally: JDK 17 and Gradle 8.11.1, then `gradle assembleDebug`.

For a consistently signed release, add these repository secrets (Settings → Secrets and variables → Actions): `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Pushing a tag such as `v0.4.1` attaches the signed APK to a GitHub Release.

### License and third-party projects

[Apache License 2.0](LICENSE). Uses [libxposed](https://github.com/libxposed) (Apache-2.0), the [wireguard-android](https://git.zx2c4.com/wireguard-android) tunnel library (Apache-2.0), AndroidX and Jetpack Compose (Apache-2.0).

### Disclaimer

This project is intended for privacy and testing. Complying with applicable laws and each service's terms of use is the user's responsibility. The project is not affiliated with Proton, TikTok, Telegram, WhatsApp, Instagram, YouTube or LSPosed. The software is provided "as is".
