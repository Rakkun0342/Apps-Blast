package com.example.blastjargas.menu

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.blastjargas.R
import kotlinx.android.synthetic.main.activity_edit_tagihan.*

class EditTagihanActivity : AppCompatActivity() {

    private lateinit var pref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_tagihan)
        setSupportActionBar(toolbarEditTagihan)
        assert(supportActionBar != null)
        supportActionBar?.title = "Edit Teks Tagihan"

        pref = getSharedPreferences("PesanWa", 0)
        editor = pref.edit()

        etTeksBottom.setText("Mohon pembayaran dilakukan melalui pembayaran resmi Pelayanan Gas Bumi Kota Depok selambat-lambatnya tanggal 20 setiap bulannya. Abaikan pesan ini jika sudah melakukan pembayaran.\nTerima kasih\n\n Pengelola Jargas Kota Depok\n *Contak Center : 0811-1977-774.*\nDimohonkan untuk tidak reply pesan ini")
        val teksHeader = pref.getString("HEADER","")
        val teksBottom = pref.getString("BOTTOM","")
        if (teksHeader != "" && teksBottom != ""){
            etTeksHeader.setText(teksHeader)
            etTeksBottom.setText(teksBottom)
        }

        btnSubmit.setOnClickListener{
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Apakah anda mau simpan perubahan?")
            builder.setPositiveButton("Ya"){_,_->
                editor.putString("HEADER", etTeksHeader.text.toString())
                editor.putString("BOTTOM", etTeksBottom.text.toString())
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