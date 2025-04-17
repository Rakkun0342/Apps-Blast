package com.example.blastjargas.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import net.sourceforge.jtds.jdbc.DefaultProperties.DATABASE_NAME

class DbHelper(context: Context): SQLiteOpenHelper(context, DATABASE_NAME, null, 1) {

    companion object {
        const val DATABASE_NAME = "BlastJargas"

        private const val CREATE_TABLE_SELESAI = "CREATE TABLE ${DbContract.NoteColumns.TABEL_SELESAI}" +
                " (${DbContract.NoteColumns.ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                " ${DbContract.NoteColumns.REGNO} TEXT NOT NULL)"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(CREATE_TABLE_SELESAI)
    }

    override fun onUpgrade(db: SQLiteDatabase?, p1: Int, p2: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $CREATE_TABLE_SELESAI")
        onCreate(db)
    }

}