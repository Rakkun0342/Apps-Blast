package com.example.blastjargas

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.example.blastjargas.menu.EditInformasiActivity
import com.example.blastjargas.menu.EditTagihanActivity
import com.example.blastjargas.menu.InformasiActivity
import com.example.blastjargas.menu.TagihanActivity
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : AppCompatActivity(), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        setSupportActionBar(toolbarHome)
        assert(supportActionBar != null)
        supportActionBar?.title = "Menu"

        imgTagihan.setOnClickListener(this)
        imgInformasi.setOnClickListener(this)
        editTagihan.setOnClickListener(this)
        editInformasi.setOnClickListener(this)
    }

    override fun onClick(v: View?){
        when(v?.id) {
            R.id.imgTagihan -> {
                intenActivity(TagihanActivity())
            }
            R.id.imgInformasi -> {
                intenActivity(InformasiActivity())
            }
            R.id.editInformasi -> {
                intenActivity(EditInformasiActivity())
            }
            R.id.editTagihan -> {
                intenActivity(EditTagihanActivity())
            }
        }
    }

    private fun intenActivity(activity: Activity){
        val intent = Intent(this, activity::class.java)
        startActivity(intent)
    }
}