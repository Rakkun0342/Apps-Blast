![image](https://github.com/user-attachments/assets/17a500b7-d342-4d46-80cc-d26c13800942)

# Apps-Blast 📲💥

**Apps-Blast** adalah aplikasi mobile Android yang dirancang khusus untuk mengirim pesan WhatsApp secara banyak ke pelanggan dengan cepat, efisien, dan otomatis.

Aplikasi ini sangat cocok untuk pelaku usaha, tim marketing, atau customer support yang ingin menjangkau banyak pelanggan hanya dengan sekali klik — langsung dari perangkat Android.

---

## 🚀 Fitur Unggulan

- ✅ Kirim pesan WhatsApp secara **banyak dan otomatis**
- ✅ Sistem berjalan di **background thread**, tetap berjalan meski layar terkunci
- ✅ Arsitektur **MVVM** yang rapi dan maintainable
- ✅ Terintegrasi dengan **SQL Server** untuk pengelolaan data customer yang kuat
- ✅ Dukungan pengiriman berbasis **template pesan**
- ✅ Riwayat dan status pengiriman pesan

---

## 🛠️ Teknologi yang Digunakan

- **Kotlin** (Native Android)
- **MVVM Architecture**
- **Coroutines + Background Thread**
- **SQL Server** (Remote Database)
- **WhatsApp Intent Integration**

---

## 🧠 Cara Kerja Singkat

1. Aplikasi mengambil daftar nomor dan pesan dari SQL Server.
2. Menggunakan `background thread` untuk mengirim pesan satu per satu via WhatsApp API.
3. Proses berjalan otomatis di latar belakang hingga semua pesan terkirim.
