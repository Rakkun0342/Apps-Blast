package com.example.blastjargas.db

import android.provider.BaseColumns

class DbContract {
    internal class NoteColumns: BaseColumns {
        companion object{
            const val TABEL_SELESAI = "SELESAI"
            const val ID = "Id"
            const val REGNO = "IdPelanggan"
        }
    }
}