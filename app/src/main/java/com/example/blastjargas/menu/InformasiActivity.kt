package com.example.blastjargas.menu

import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.blastjargas.R
import com.example.blastjargas.adapter.ListStatusAdapter
import com.example.blastjargas.adapter.TunggakanAdapter
import com.example.blastjargas.db.DbContract
import com.example.blastjargas.db.DbQuery
import com.example.blastjargas.model.Status
import com.example.blastjargas.model.Tunggakan
import com.example.blastjargas.sql.ConnectSQL
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.loopj.android.http.AsyncHttpResponseHandler
import com.loopj.android.http.SyncHttpClient
import cz.msebera.android.httpclient.Header
import cz.msebera.android.httpclient.entity.StringEntity
import kotlinx.android.synthetic.main.activity_informasi.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.sql.SQLException

class InformasiActivity : AppCompatActivity() {

    private lateinit var listStatusAdapter: ListStatusAdapter
    private lateinit var tunggakanAdapter: TunggakanAdapter
    private lateinit var connectSQL: ConnectSQL
    private lateinit var bottomSheetDialog: BottomSheetDialog

    private lateinit var dbQuery: DbQuery
    private lateinit var mTunggakan: MutableList<Tunggakan>
    private lateinit var mSector: MutableList<Status>
    private var selectedSector : String? = null
    private lateinit var pref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private var arrayGagal = ArrayList<String>()

    private var dataKirim: MutableList<Tunggakan> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_informasi)
        setSupportActionBar(toolbarInformasi)
        assert(supportActionBar != null)
        supportActionBar?.title = "Data Pelanggan"

        pref = getSharedPreferences("PesanWaInformasi", 0)
        editor = pref.edit()

        connectSQL = ConnectSQL()

        rvItems.layoutManager = LinearLayoutManager(this)

        showList(null)

        swipeRefresh.setOnRefreshListener {
            tunggakanAdapter.notifyDataSetChanged()
            showList(null)
        }

        etSearch.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                imageClear.visibility = View.VISIBLE
                tunggakanAdapter.filter.filter(etSearch.text)
                hideKeyboard(this)
            }
            false
        }

        imageClear.setOnClickListener{
            etSearch.text.clear()
            imageClear.visibility = View.GONE
            tunggakanAdapter.filter.filter(etSearch.text)
        }

        imgFilter.setOnClickListener{
            val dialogView = layoutInflater.inflate(R.layout.bottom_shet_filter, null)
            val rvListSector = dialogView.findViewById<RecyclerView>(R.id.rvListSector)
            rvListSector.layoutManager = LinearLayoutManager(this)
            listStatusAdapter = ListStatusAdapter(mSector)
            rvListSector.adapter = listStatusAdapter
            listStatusAdapter.setOnItemClickCallback(object : ListStatusAdapter.OnItemClickCallback{
                override fun onItemClicked(sector: Status) {
                    bottomSheetDialog.dismiss()
                    selectedSector = sector.status.toString()
                    showList(sector.status)
                }
            })
            bottomSheetDialog = BottomSheetDialog(this@InformasiActivity)
            bottomSheetDialog.setContentView(dialogView)
            bottomSheetDialog.show()
        }
        btnCari.setOnClickListener {
            showList(selectedSector)
        }
    }

    private fun hideKeyboard(activity: Activity) {
        val view = activity.findViewById<View>(android.R.id.content)
        if (view != null) {
            val imm = activity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun showList(sec: String?){
        val progressBar = ProgressDialog(this)
        progressBar.setTitle("Mohon tunggu sebentar")
        progressBar.setMessage("Sedang mengambil data")
        progressBar.setCanceledOnTouchOutside(false)
        progressBar.show()
        lifecycleScope.launch{
            val result = withContext(Dispatchers.IO){
                loadItem(sec)
            }
            when (result){
                "berhasil" -> {
                    progressBar.dismiss()
                    swipeRefresh.setRefreshing(false)
                    tunggakanAdapter = TunggakanAdapter(mTunggakan)
                    rvItems.adapter = tunggakanAdapter
                    tunggakanAdapter.setOnItemClickCallback(object : TunggakanAdapter.OnItemClickCallback{
                        override fun onItemClicked(items: Tunggakan, position: Int) {
                            val builder = AlertDialog.Builder(this@InformasiActivity)
                            if (items.selesai == "TANDA"){
                                builder.setTitle("Apakah anda mau hapus ${items.namaTunggakan} dari list kirim?")
                                builder.setPositiveButton("Hapus"){_,_->
                                    lifecycleScope.launch(Dispatchers.Main){
                                        dataKirim.remove(items)
                                        mTunggakan[position] = Tunggakan(items.idTunggakan, items.noMeter, items.sectorTunggakan, items.namaTunggakan, items.alamatTunggakan, items.phoneTunggakan, items.tipeTunggakan, items.tagihanTunggakan, items.biayaTunggakan, items.rincianTunggakan, items.returnPPnTunggakan, items.dendaTunggakan, items.jumlahTunggakan, "")
                                        tunggakanAdapter.notifyDataSetChanged()
                                    }
                                }
                                builder.setNegativeButton("Tidak"){p,_->
                                    p.dismiss()
                                }
                                builder.create().show()
                            }else{
                                builder.setTitle("Apakah anda mau menambahkan ${items.namaTunggakan} kedalam list kirim?")
                                builder.setPositiveButton("Tambah"){_,_->
                                    dataKirim.add(Tunggakan(items.idTunggakan, items.noMeter,
                                        items.sectorTunggakan, items.namaTunggakan,
                                        items.alamatTunggakan, items.phoneTunggakan,
                                        items.tipeTunggakan, items.tagihanTunggakan,
                                        items.biayaTunggakan, items.rincianTunggakan,
                                        items.returnPPnTunggakan, items.dendaTunggakan, items.jumlahTunggakan, "TANDA"))
                                    mTunggakan[position] = Tunggakan(items.idTunggakan, items.noMeter,
                                        items.sectorTunggakan, items.namaTunggakan, items.alamatTunggakan,
                                        items.phoneTunggakan, items.tipeTunggakan, items.tagihanTunggakan,
                                        items.biayaTunggakan, items.rincianTunggakan, items.returnPPnTunggakan,
                                        items.dendaTunggakan, items.jumlahTunggakan, "TANDA")
                                    tunggakanAdapter.notifyDataSetChanged()
                                }
                                builder.setNegativeButton("Tidak"){p,_->
                                    p.dismiss()
                                }
                                builder.create().show()
                            }
                        }
                    })
                }
                "gagal" -> {
                    progressBar.dismiss()
                    swipeRefresh.setRefreshing(false)
                    Toast.makeText(this@InformasiActivity, "Gagal mengambil data", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadItem( sec: String?): String{
        val conn = connectSQL.connection()
        mTunggakan = mutableListOf<Tunggakan>()
        mSector = mutableListOf()
        val sector = if (sec.isNullOrEmpty()){
            ""
        }else{
            "C.Sector = '$sec' AND"
        }
        if (conn != null){
            try {
                val querySector = "SELECT\n" +
                        "\tC.Sector, \n" +
                        "\tCOUNT(C.RegNo) AS Jumlah\n" +
                        "FROM J_Cust C       \n" +
                        "LEFT JOIN VIEW_J_Cust S ON C.CustID = S.CustID\n" +
                        "WHERE C.Phone != '' AND S.Status = 'Aktif'\n" +
                        "GROUP BY C.Sector\n"
                val csCater = conn.createStatement()
                val resulCater = csCater.executeQuery(querySector)
                while (resulCater.next()){
                    val modelStatus = Status()
                    modelStatus.status = resulCater.getString("Sector")
                    modelStatus.semua = resulCater.getInt("Jumlah")
                    mSector.add(modelStatus)
                }
                val query = "SELECT C.RegNo, S.NoMeter AS NoMeter, C.Sector, C.CName AS Nama,\n" +
                        "\tC.Phone,\n" +
                        "\tC.FullAddress AS AlamatLengkap, \n" +
                        "\tC.CType AS Tipe\n" +
                        "FROM J_Cust C       \n" +
                        "LEFT JOIN VIEW_J_Cust S ON C.CustID = S.CustID\n" +
                        "WHERE $sector C.Phone != '' AND S.Status = 'Aktif'\n"
                val createStatemen = conn.prepareStatement(query)
                val resultSet = createStatemen.executeQuery()
                while (resultSet.next()){
                    val tunggakan = Tunggakan()
                    tunggakan.idTunggakan = resultSet.getString("RegNo")
                    tunggakan.noMeter = resultSet.getString("NoMeter")
                    tunggakan.sectorTunggakan = resultSet.getString("Sector")
                    tunggakan.namaTunggakan = resultSet.getString("Nama")
                    tunggakan.alamatTunggakan = resultSet.getString("AlamatLengkap")
                    tunggakan.phoneTunggakan = resultSet.getString("Phone")
                    tunggakan.tipeTunggakan = resultSet.getString("Tipe")
                    mTunggakan.add(tunggakan)
                }
                return "berhasil"
            }catch (e: SQLException){
                e.printStackTrace()
                return "gagal"
            }
        }
        return "gagal"
    }

    private suspend fun sendWa(phone: String, nama: String): String {
        return suspendCancellableCoroutine { continuation ->

            val teksBottom = pref.getString("TEKS","")

            var teksInformasi = "Pelanggan Yth. *$nama* \n" +
                    "Bahwasanya Loket Pembayaran Gas akan libur menjelang Ramadhan sampai tanggal 20/04/2024 \n" +
                    "Pengelola Jargas Depok"

            if (teksBottom != ""){
                teksInformasi = "Pelanggan Yth. *$nama* \n" + teksBottom.toString()
            }

            val client = SyncHttpClient()
            client.setTimeout(30000) // 30 Detik
//            val url = "http://116.203.191.58/api/send_message"
            val url = "https://api.watzap.id/v1/send_message"
            val jsonParams = JSONObject()
            jsonParams.put("phone_no", phone)
            jsonParams.put("api_key", "")
            jsonParams.put("message", teksInformasi)
            jsonParams.put("number_key", "")
//            jsonParams.put("flag_retry", "on")
//            jsonParams.put("pendingTime", 5)
            val entity = StringEntity(jsonParams.toString())

            client.post(this@InformasiActivity, url, entity, "application/json", object : AsyncHttpResponseHandler() {
                override fun onSuccess(statusCode: Int, headers: Array<out Header>?, responseBody: ByteArray?) {
                    continuation.resumeWith(Result.success("$nama berhasil"))
                }

                override fun onFailure(statusCode: Int, headers: Array<out Header>?, responseBody: ByteArray?, error: Throwable?) {
                    Log.e("status code", statusCode.toString())
                    Log.e("eror", error.toString())
                    Log.e("response", responseBody.toString())
                    continuation.resumeWith(Result.success("$nama $error"))
                }
            })
            continuation.invokeOnCancellation {
                client.cancelAllRequests(true)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.send, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sendWaNoAcc -> {
                Toast.makeText(this, "Fitur ini tidak tersedia", Toast.LENGTH_LONG).show()
            }
            R.id.sendWa -> {
                lifecycleScope.launch{
                    var i = 0
                    val result = withContext(Dispatchers.IO){
                        mTunggakan.forEach{ _ ->
                            i++
                        }
                        return@withContext i
                    }
                    val builder = AlertDialog.Builder(this@InformasiActivity)
                    builder.setTitle("Apakah anda yakin kirim pesan sebanyak $result")
                    builder.setPositiveButton("Ya"){_,_->
                        val progressBar = ProgressDialog(this@InformasiActivity)
                        progressBar.setCanceledOnTouchOutside(false)
                        progressBar.setCancelable(false)
                        progressBar.setTitle("Mohon tunggu sebentar")
                        progressBar.max = result
                        progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                        progressBar.show()
                        var terkirim = 0
                        lifecycleScope.launch(Dispatchers.Default) {
                            try {
                                mTunggakan.forEach {
                                    withContext(Dispatchers.Main) {
                                        progressBar.progress = terkirim
                                    }
                                    delay(10000) //10 detik
                                    val hasil = sendWa(it.phoneTunggakan.toString(), it.namaTunggakan.toString())
//                                    Log.e("sudah terkirim", terkirim.toString())
//                                    Log.e("hasil send Wa", hasil)
//                                    Log.e("nomor send wa", it.phoneTunggakan.toString())
                                    terkirim++
                                    if (hasil == "${it.namaTunggakan} berhasil"){
//                                        withContext(Dispatchers.IO){
//                                            val contentValues = ContentValues()
//                                            contentValues.put(DbContract.NoteColumns.REGNO, it.idTunggakan)
//                                            dbQuery.insert(contentValues)
//                                        }
                                    }else{
                                        withContext(Dispatchers.Main){
                                            arrayGagal.add(it.namaTunggakan.toString())
                                            Log.e("Erorr balasan", hasil)
                                        }
                                    }
                                }
                            } finally {
                                withContext(Dispatchers.Main) {
                                    progressBar.dismiss()
                                    val builder = AlertDialog.Builder(this@InformasiActivity)
                                    builder.setTitle("List Gagal")
                                    builder.setMessage(arrayGagal.joinToString("\n") {
                                        it.toString()
                                    })
                                    builder.setPositiveButton("OK"){p,_->
                                        arrayGagal.clear()
                                        p.dismiss()
                                    }
                                    builder.create().show()
                                }
                            }
                        }
                    }
                    builder.setNegativeButton("Tidak"){p,_->
                        p.dismiss()
                    }
                    builder.create().show()
                }
            }
            R.id.sendWaArcive -> {
                lifecycleScope.launch{
                    val result = withContext(Dispatchers.IO){
                        var i = 0
                        dataKirim.forEach{ _ ->
                            i++
                        }
                        return@withContext i
                    }
                    val builder = AlertDialog.Builder(this@InformasiActivity)
                    builder.setTitle("Apakah anda yakin kirim pesan sebanyak $result")
                    builder.setPositiveButton("Ya"){_,_->
                        val progressBar = ProgressDialog(this@InformasiActivity)
                        progressBar.setTitle("Mohon tunggu sebentar")
                        progressBar.setCanceledOnTouchOutside(false)
                        progressBar.setCancelable(false)
                        progressBar.max = result
                        progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                        progressBar.show()
                        var terkirim = 0
                        lifecycleScope.launch(Dispatchers.Default) {
                            try {
                                val listData = dataKirim.iterator()
                                while (listData.hasNext()) {
                                    val it = listData.next()
                                    withContext(Dispatchers.Main) {
                                        progressBar.progress = terkirim
                                    }
                                    delay(10000)
                                    val hasil = sendWa(it.phoneTunggakan.toString(), it.namaTunggakan.toString())
                                    Log.e("hasil send Wa", hasil)
                                    terkirim++
                                    if (hasil == "${it.namaTunggakan} berhasil"){
//                                        withContext(Dispatchers.IO){
//                                            val contentValues = ContentValues()
//                                            contentValues.put(DbContract.NoteColumns.REGNO, it.idTunggakan)
//                                            dbQuery.insert(contentValues)
//                                        }
                                        Log.e("hasil eksekusi", true.toString())
                                        withContext(Dispatchers.Main){
                                            listData.remove()
                                        }
                                    }
                                    else{
                                        withContext(Dispatchers.Main){
                                            arrayGagal.add(it.namaTunggakan.toString())
                                            Log.e("Erorr balasan", hasil)
                                            listData.remove()
                                        }
                                    }
                                }
                            } finally {
                                withContext(Dispatchers.Main) {
                                    progressBar.dismiss()
                                    val builder = AlertDialog.Builder(this@InformasiActivity)
                                    builder.setTitle("List Gagal")
                                    builder.setMessage(arrayGagal.joinToString("\n") {
                                        it.toString()
                                    })
                                    builder.setPositiveButton("OK"){p,_->
                                        arrayGagal.clear()
                                        p.dismiss()
                                    }
                                    builder.create().show()
                                }
                            }
                        }
                    }
                    builder.setNegativeButton("Tidak"){p,_->
                        p.dismiss()
                    }
                    builder.create().show()
                }
            }
        }
        return true
    }
}