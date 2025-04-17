package com.example.blastjargas.menu

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.blastjargas.R
import kotlinx.android.synthetic.main.activity_edit_informasi.*
import kotlinx.android.synthetic.main.activity_edit_tagihan.btnSubmit
import kotlinx.android.synthetic.main.activity_edit_tagihan.etTeksBottom
import kotlinx.android.synthetic.main.activity_edit_tagihan.etTeksHeader

class EditInformasiActivity : AppCompatActivity() {

    private lateinit var pref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_informasi)
        setSupportActionBar(toolbarEditInformasi)
        assert(supportActionBar != null)
        supportActionBar?.title = "Edit Teks Informasi"

        pref = getSharedPreferences("PesanWaInformasi", 0)
        editor = pref.edit()

        val teksBottom = pref.getString("TEKS","")
        if (teksBottom != ""){
            etTeksBottom.setText(teksBottom)
        }

        btnSubmit.setOnClickListener{
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Apakah anda mau simpan perubahan?")
            builder.setPositiveButton("Ya"){_,_->
                editor.putString("TEKS", etTeksBottom.text.toString())
                editor.commit()
                Toast.makeText(this, "Perubahan berhasil disimpan", Toast.LENGTH_LONG).show()
                finish()
            }
            builder.setNegativeButton("Tidak"){p,_->
                Toast.makeText(this, "Perubahan gagal disimpan", Toast.LENGTH_LONG).show()
                p.dismiss()
            }
            builder.create().show()
        }
    }
}