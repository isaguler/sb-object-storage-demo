# sb-object-storage-demo

Spring Boot 3.5 + SeaweedFS (S3 uyumlu object storage) PoC.

Akış: endpoint dummy bir Excel üretir, AWS SDK v2 ile SeaweedFS'e yükler. İkinci endpoint bu obje için süreli bir presigned URL verir. Dosya bu URL'den indirilir.

## Gereksinimler

- Java 21
- Docker Desktop
- Boş portlar: `8333` (S3 API), `8888` (filer UI), `8080` (uygulama)

## Çalıştırma

```bash
docker compose up -d
```

```bash
./mvnw spring-boot:run
```

Windows'ta `mvnw.cmd spring-boot:run`. SeaweedFS `demo-bucket` bucket'ını açılışta kendisi oluşturur.

## Demo

1. Excel üret ve yükle (`201` döner):

```bash
curl -X POST http://localhost:8080/api/files/excel
```

```json
{"key":"reports/3365e2ac-40dc-4966-9299-e1be2960bf6b.xlsx"}
```

2. Presigned URL al:

```bash
curl "http://localhost:8080/api/files/presigned-url?key=reports/3365e2ac-40dc-4966-9299-e1be2960bf6b.xlsx"
```

```json
{"url":"http://localhost:8333/demo-bucket/reports/...xlsx?X-Amz-Algorithm=...&X-Amz-Signature=...","expiresAt":"2026-10-23T08:52:50Z"}
```

3. `url` değerini tarayıcıda aç; dosya iner.

Göstermek için ek kontroller:

- **İmzasız erişim 403 döner:** `http://localhost:8333/demo-bucket/reports/<uuid>.xlsx`
- **İmzası bozulmuş ya da süresi dolmuş URL 403 döner.** Süresi dolanda mesaj `Request has expired` olur. Hızlı denemek için uygulamayı `STORAGE_PRESIGN_EXPIRY=5s` ile başlat.
- **Yüklenen dosyalar:** http://localhost:8888/buckets/demo-bucket/reports/ (filer UI)

Kapatmak için:

```bash
docker compose down
```

Verileri de silmek için `docker compose down -v`.

## Konfigürasyon

`application.yaml` içindeki `storage.*` ayarları env var ile ezilebilir:

| Property | Env var | Varsayılan |
|---|---|---|
| `storage.endpoint` | `STORAGE_ENDPOINT` | `http://localhost:8333` |
| `storage.public-endpoint` | `STORAGE_PUBLIC_ENDPOINT` | `endpoint` ile aynı |
| `storage.region` | `STORAGE_REGION` | `us-east-1` |
| `storage.access-key` | `STORAGE_ACCESS_KEY` | `demo-access-key` |
| `storage.secret-key` | `STORAGE_SECRET_KEY` | `demo-secret-key` |
| `storage.bucket` | `STORAGE_BUCKET` | `demo-bucket` |
| `storage.presign-expiry` | `STORAGE_PRESIGN_EXPIRY` | `10m` (SigV4 üst sınırı 7 gün) |

`public-endpoint` neden ayrı? Uygulama SeaweedFS'e iç adresle bağlanır (ör. `http://seaweedfs:8333`). Ancak presigned URL'deki host istemcinin erişebileceği bir adres olmalı. Host imzanın parçası olduğu için URL üretildikten sonra değiştirilemez; presigner doğrudan public adresle kurulur.

## Kod

| Sınıf | Görev |
|---|---|
| `FileController` | `POST /api/files/excel`, `GET /api/files/presigned-url?key=` |
| `ExcelGenerator` | fastexcel ile tek hücrelik `.xlsx` |
| `ObjectStorageService` | Dosya tipinden bağımsız `upload` ve `presignedGetUrl` |
| `S3Config` | `S3Client` ve `S3Presigner`. SeaweedFS için path-style açık. |
| `StorageProperties` | `storage.*` ayarları |

## Notlar (PoC kapsamı dışında)

**Kimlik doğrulama.** SeaweedFS'te hiç kimlik bilgisi yoksa her şey anonim olarak açıktır ve presigned URL'in anlamı kalmaz. Demo, `AWS_ACCESS_KEY_ID` ve `AWS_SECRET_ACCESS_KEY` ile tek bir admin kimliği tanımlar.

**Büyük Excel dosyaları.**
- **Kütüphane:** fastexcel bilinçli olarak seçildi. Streaming yazar ve Apache POI'ye göre çok daha az bağımlılık getirir. Zengin formatlama ya da Excel okuma gerekirse POI'nin `SXSSFWorkbook`'u kullanılmalı; `XSSFWorkbook` büyük veride OOM'a yol açar.
- **Bellek:** Demo dosyayı `byte[]` olarak bellekte tutar. Büyük raporlarda dosya geçici bir dosyaya yazılmalı (Kubernetes'te `emptyDir`) ve `RequestBody.fromFile(...)` ya da S3 Transfer Manager ile multipart yüklenmeli.
- **Satır limiti:** Excel'de sayfa başına en fazla 1.048.576 satır olabilir. Daha büyük veride birden fazla sayfaya bölünmeli.

**Cloud (self-managed) kurulum.**
- **Tek node:** `weed mini` tek node'luk bir geliştirme modudur. Prod için `weed server -s3` ya da ayrı master, volume, filer ve s3 servisleri kurulmalı, kalıcı disk bağlanmalı.
- **Yetki:** Env var ile tanımlanan kimlik global admin yetkisine sahiptir. Prod'da `s3.json` ile bucket bazlı yetkili kimlikler tanımlanmalı (ör. `Read:bucket`, `Write:bucket`).
- **Dışarı açılan portlar:** Yalnızca S3 portu (8333) dışarı açılmalı. Filer (8888), master ve volume portları S3 kimlik doğrulaması olmadan erişim verir.
- **Reverse proxy:** Önde bir reverse proxy varsa `Host` ve `X-Forwarded-*` header'ları korunmalı, aksi halde imza doğrulaması bozulur.

**Diğer.**
- **Olmayan key:** Presigned URL üretilirken objenin varlığı kontrol edilmez. Olmayan bir key için de URL üretilir, indirirken `404` döner.
- **Checksum:** AWS SDK 2.30 ve sonrası yüklemelerde varsayılan olarak CRC32 checksum gönderir. SeaweedFS 4.48 bunu sorunsuz kabul ediyor. Eski veya farklı bir S3 uyumlu sistemde sorun çıkarsa `S3Client` builder'ında `requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)` ayarı kullanılmalı.
