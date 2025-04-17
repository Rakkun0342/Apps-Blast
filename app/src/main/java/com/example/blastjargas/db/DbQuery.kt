package com.example.blastjargas.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor

class DbQuery(context: Context) {
    private var db = DbHelper(context).writableDatabase
    companion object {
        private const val NAME_TABEL = DbContract.NoteColumns.TABEL_SELESAI
        private var INSTANCE: DbQuery? = null

        fun getInstance(context: Context): DbQuery =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: DbQuery(context)
            }
    }

    fun close() {
        db.close()

        if (db.isOpen)
            db.close()
    }

    fun insert(values: ContentValues?): Long {
        return db.insert(NAME_TABEL, null, values)
    }

    fun update(id: String, values: ContentValues?): Int{
        return db.update(NAME_TABEL, values, "${DbContract.NoteColumns.REGNO} = ?", arrayOf(id))
    }

    fun delete() {
        db.delete(NAME_TABEL, null, null)
    }

    fun queryAll(): Cursor {
        return db.query(
            NAME_TABEL,
            null,
            null,
            null,
            null,
            null,
            null
        )
    }

    fun cekId(id: String):String{
        val query = db.rawQuery("SELECT CASE WHEN IdPelanggan = '$id' THEN 'ADA' ELSE 'KOSONG' END AS Result FROM $NAME_TABEL WHERE IdPelanggan = '$id'", null)
        query.apply {
            while (moveToNext()){
                return getString(getColumnIndexOrThrow("Result"))
            }
        }
        return "KOSONG"
    }
}