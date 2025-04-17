package com.example.blastjargas.menu

import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.SharedPreferences
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
import androidx.appcompat.app.AppCompatActivity
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
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.sql.SQLException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class TagihanActivity : AppCompatActivity() {

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

    private var spanChat = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        assert(supportActionBar != null)
        supportActionBar?.title = "Tagihan"
        dbQuery = DbQuery.getInstance(this)

        pref = getSharedPreferences("PesanWa", 0)
        editor = pref.edit()

        connectSQL = ConnectSQL()

        rvItems.layoutManager = LinearLayoutManager(this)
//        progress.visibility = View.VISIBLE

        val formatter = SimpleDateFormat("dd")
        val current = formatter.format(Calendar.getInstance().time)

        Log.e("cek konfirm", pref.getBoolean("Konfirm",false).toString())
        if (pref.getBoolean("Konfirm",false)){
            if (current == "01"){
                lifecycleScope.launch{
                    dbQuery.delete()
                    editor.putBoolean("Konfirm", false)
                    editor.commit()
                    showList(null)
//                    Log.e("CekDate", "1")
                }
            }else{
                showList(null)
//                Log.e("CekDate", "2")
            }
        }else{
            if (current != "01"){
                editor.putBoolean("Konfirm", true)
                editor.commit()
                showList(null)
//                Log.e("CekDate", "3")
            }else{
                showList(null)
//                Log.e("CekDate", "4")
            }
        }

        lifecycleScope.launch{
            val resultT = withContext(Dispatchers.IO){
                getMaxT()
            }
            etTo.setText(resultT.toString())
        }

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
            bottomSheetDialog = BottomSheetDialog(this@TagihanActivity)
            bottomSheetDialog.setContentView(dialogView)
            bottomSheetDialog.show()
        }

        btnCari.setOnClickListener {
            showList(selectedSector)
        }

    }

    private fun getMaxT():Int{
        val conn = connectSQL.connection()
        var resultT = 0
        if (conn != null){
            try {
                val queryMaxTagihan = "SELECT MAX(T.Tagihan) As Jumlah \n" +
                        "FROM (SELECT \n" +
                        "    COUNT(C.RegNo) AS Tagihan\n" +
                        "FROM J_Billing B            \n" +
                        "JOIN J_Cust C ON C.CustID = B.CustID  \n" +
                        "LEFT JOIN VIEW_J_Cust S ON C.CustID = S.CustID\n" +
                        "LEFT JOIN dbo.VIEW_J_Receipt_EffPaid EP ON EP.ItemType = 'TG' AND EP.ItemID = B.BillingID          \n" +
                        "WHERE EP.TPaid IS NULL AND B.ApprovedDateTime IS NOT NULL AND B.VoidDateTime IS NULL AND  S.Status = 'Aktif' \n" +
                        "GROUP BY C.RegNo) T"
                val csT = conn.createStatement()
                val resulT = csT.executeQuery(queryMaxTagihan)
                while (resulT.next()){
                    resultT = resulT.getInt("Jumlah")
                }
                return resultT
            }catch (e: SQLException){
                e.printStackTrace()
            }
        }
        return resultT
    }

    private fun showList(sec: String?){
        val getFrom = etFrom.text.toString().trim()
        val getTo = etTo.text.toString().trim()
        val progressBar = ProgressDialog(this)
        progressBar.setTitle("Mohon tunggu sebentar")
        progressBar.setMessage("Sedang mengambil data")
        progressBar.setCanceledOnTouchOutside(false)
        progressBar.show()
        lifecycleScope.launch{
            val result = withContext(Dispatchers.IO){
                loadItem(getFrom, getTo, sec)
            }
            when (result){
                "berhasil" -> {
                    for ((i, items) in mTunggakan.withIndex()){
                        val selesai = withContext(Dispatchers.IO){
                            dbQuery.cekId(items.idTunggakan.toString())
                        }
                        if (selesai == "ADA"){
                            mTunggakan[i] = Tunggakan(items.idTunggakan, items.noMeter,
                                items.sectorTunggakan, items.namaTunggakan,
                                items.alamatTunggakan, items.phoneTunggakan,
                                items.tipeTunggakan, items.tagihanTunggakan, items.biayaTunggakan, items.rincianTunggakan, items.returnPPnTunggakan, items.dendaTunggakan, items.jumlahTunggakan, "ADA")
                        }
                        withContext(Dispatchers.IO){
                            dataKirim.forEach {
                                if (items.idTunggakan == it.idTunggakan){
                                    mTunggakan[i] = it
                                }
                            }
                        }
                    }
                    progressBar.dismiss()
                    mTunggakan.sortBy { it.selesai }
                    tunggakanAdapter = TunggakanAdapter(mTunggakan)
                    rvItems.adapter = tunggakanAdapter
                    tunggakanAdapter.setOnItemClickCallback(object : TunggakanAdapter.OnItemClickCallback{
                        override fun onItemClicked(items: Tunggakan, position: Int) {
                            val builder = AlertDialog.Builder(this@TagihanActivity)
                            if (items.selesai == "TANDA"){
                                builder.setTitle("Apakah anda mau hapus ${items.namaTunggakan} dari list kirim?")
                                builder.setPositiveButton("Hapus"){_,_->
                                    lifecycleScope.launch(Dispatchers.Main){
                                        val selesai = withContext(Dispatchers.IO){
                                            dbQuery.cekId(items.idTunggakan.toString())
                                        }
                                        dataKirim.remove(items)
                                        if (selesai == "ADA"){
                                            mTunggakan[position] = Tunggakan(items.idTunggakan, items.noMeter,
                                                items.sectorTunggakan, items.namaTunggakan,
                                                items.alamatTunggakan, items.phoneTunggakan,
                                                items.tipeTunggakan, items.tagihanTunggakan,
                                                items.biayaTunggakan, items.rincianTunggakan,
                                                items.returnPPnTunggakan, items.dendaTunggakan, items.jumlahTunggakan, "ADA")
                                        }else{
                                            mTunggakan[position] = Tunggakan(items.idTunggakan, items.noMeter, items.sectorTunggakan, items.namaTunggakan, items.alamatTunggakan, items.phoneTunggakan, items.tipeTunggakan, items.tagihanTunggakan, items.biayaTunggakan, items.rincianTunggakan, items.returnPPnTunggakan, items.dendaTunggakan, items.jumlahTunggakan, "")
                                        }
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
                    swipeRefresh.setRefreshing(false)
                }
                "gagal" -> {
                    progressBar.dismiss()
                    swipeRefresh.setRefreshing(false)
                    Toast.makeText(this@TagihanActivity, "Gagal mengambil data", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun hideKeyboard(activity: Activity) {
        val view = activity.findViewById<View>(android.R.id.content)
        if (view != null) {
            val imm = activity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun loadItem(from: String, to: String, sec: String?): String{
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
                val querySector = "SELECT \n" +
                        "    C.Sector, \n" +
                        "    COUNT(C.RegNo) AS Tagihan\n" +
                        "FROM J_Billing B            \n" +
                        "JOIN J_Cust C ON C.CustID = B.CustID  \n" +
                        "LEFT JOIN VIEW_J_Cust S ON C.CustID = S.CustID\n" +
                        "LEFT JOIN dbo.VIEW_J_Receipt_EffPaid EP ON EP.ItemType = 'TG' AND EP.ItemID = B.BillingID          \n" +
                        "WHERE EP.TPaid IS NULL AND B.ApprovedDateTime IS NOT NULL AND B.VoidDateTime IS NULL AND  S.Status = 'Aktif' \n" +
                        "GROUP BY C.Sector"
                val csCater = conn.createStatement()
                val resulCater = csCater.executeQuery(querySector)
                while (resulCater.next()){
                    val modelStatus = Status()
                    modelStatus.status = resulCater.getString("Sector")
                    modelStatus.semua = resulCater.getInt("Tagihan")
                    mSector.add(modelStatus)
                }
                val query = "SELECT \n" +
                        "    C.RegNo, S.NoMeter AS NoMeter, C.Sector, \n" +
                        "    C.CName AS Nama,\n" +
                        "    C.Phone,\n" +
                        "    C.FullAddress AS AlamatLengkap, \n" +
                        "    C.CType AS Tipe,\n" +
                        "    COUNT(C.RegNo) AS Tagihan, \n" +
                        "    SUM(B.Amount) AS Biaya, \n" +
                        "    SUM(B.TotalTaxRefund) AS ReturnPPn, \n" +
                        "    SUM(B.TotalFine) AS Denda,\n" +
                        "    (SUM(B.Amount) + SUM(B.TotalFine)) - (SUM(B.TotalTaxRefund) + ISNULL(SUM(EP.TPaid), 0)) AS Jumlah,\n" +
                        "   STRING_AGG(CONCAT('Nama: ',\n" +
                        "\t\tFORMAT(CONVERT(DATE, CAST(B.Period AS VARCHAR) + '01', 112), 'MMM-yyyy'), ': ' ,\n" +
                        "\t\tCONCAT('Rp', REPLACE(FORMAT((B.Amount + B.TotalFine) - B.TotalTaxRefund, '##,##0'), ',', '.'))), '\n') As Detail " +
                        "FROM J_Billing B            \n" +
                        "JOIN J_Cust C ON C.CustID = B.CustID  \n" +
                        "LEFT JOIN VIEW_J_Cust S ON C.CustID = S.CustID\n" +
                        "LEFT JOIN dbo.VIEW_J_Receipt_EffPaid EP ON EP.ItemType = 'TG' AND EP.ItemID = B.BillingID          \n" +
                        "WHERE $sector C.Phone != '' AND EP.TPaid IS NULL AND B.ApprovedDateTime IS NOT NULL AND B.VoidDateTime IS NULL AND  S.Status = 'Aktif' \n" +
                        "GROUP BY C.RegNo, C.CName, C.FullAddress, C.CType, C.Sector, S.NoMeter, C.Phone\n" +
                        "HAVING COUNT(C.RegNo) BETWEEN ? AND ? \n" +
                        "ORDER BY COUNT(C.RegNo)"
                val createStatemen = conn.prepareStatement(query)
                createStatemen.setString(1, from)
                createStatemen.setString(2, to)
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
                    tunggakan.tagihanTunggakan = resultSet.getInt("Tagihan")
                    tunggakan.biayaTunggakan = resultSet.getInt("Biaya")
                    tunggakan.returnPPnTunggakan = resultSet.getInt("ReturnPPn")
                    tunggakan.dendaTunggakan = resultSet.getInt("Denda")
                    tunggakan.jumlahTunggakan = resultSet.getInt("Jumlah")
                    tunggakan.rincianTunggakan = resultSet.getString("Detail")
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

    private fun formatRupiah(): NumberFormat {
        val localeID = Locale("in", "ID")
        return NumberFormat.getCurrencyInstance(localeID)
    }

    private suspend fun sendWa(phone: String, span: Int, id: String, nama: String, alamat: String, sector: String, bulan: String, rincian: String, total: String): String {
        return suspendCancellableCoroutine { continuation ->
            val getDate = when(span){
                1 -> {
                    SimpleDateFormat("MMM/yyyy").format(Date())
                }
                2 -> {
                    SimpleDateFormat("MMM-yyyy").format(Date())
                }
                else -> {
                    SimpleDateFormat("MM/yyyy").format(Date())
                }
            }
            val span1 = when(span){
                1 -> {
                    "Hay, Pelanggan setia Jargas Kota Depok \n" +
                            "Kami dari pengelola Jargas Depok akan memberikan infromasi mengenai tagihan gas rumah tangga sebagai berikut: \n"
                }
                2 -> {
                    "Hallo, Pengguna setia Jargas Kota Depok \n" +
                            "Kami dari pengelola Jargas Depok ingin menginformasikan terkait tagihan gas rumah tangga sebagai berikut: \n"
                }
                else -> {
                    "Halo, Customer setia Jargas Kota Depok \n" +
                            "Kami dari pengelola Jargas Depok akan memberitahukan tagihan gas alam rumah tangga sebagai berikut: \n"
                }
            }
            val span2 = when(span){
                1 -> {
                    "\nDengan hormat agar segera dilakukan pembayaran sebelum tanggal 20/$getDate *agar tidak dikenakan denda tambahan*. \n" +
                            "Demikian informasi yang diberikan, Terima Kasih Atas Perhatiannya. \n" +
                            "\nSalam Hormat, \n" +
                            "Pengelola Jargas Kota Depok \n" +
                            "*NB: Ada yang mau ditanyakan?, dapat menghubungi nomor berikut: 0811-1977-774.*\n" +
                            "DiHarapkan untuk tidak mengirim pesan ke nomor ini"
                }
                2 -> {
                    "\nDemikan informasi tagihan yang diberikan, mohon pembayaran dilakukan sebelum tanggal 20-$getDate *agar tidak dikenakan denda tambahan*. \n" +
                            "Atas Perhatiannya kami ucapkan terimakasih. \n" +
                            "\nSalam Hangat, \n" +
                            "Pengelola Jargas Kota Depok \n" +
                            "*NB: Nomor ini hanya memberikan Informasi tagihan, ada pertanyaan? dapat menghubungi nomor berikut: 0811-1977-774.*\n" +
                            "DiMohonkan untuk tidak mengirim pesan ke nomor ini"
                }
                else -> {
                    "\nDemikian pemberitahuan terkait tagihan yang diberikan, Mohon untuk pembayaran dilakukan sebelum 20/$getDate *agar tidak mendapatkan denda tambahan*. \n" +
                            "Terimakasih banyak atas perhatian yang telah diberikan.\n" +
                            "\nSalam Sejahtera, \n" +
                            "Pengelola Jargas Kota Depok \n" +
                            "*NB: Ada pertanyaan? terkait tagihan atau yang lain, dapat menghubungi nomor berikut: 0811-1977-774.*\n" +
                                    "Mohon untuk tidak mengirim pesan ke nomor ini."
                }
            }

            val teksHeader = pref.getString("HEADER","")
            val teksBottom = pref.getString("BOTTOM","")

            var headerTeks = "Pelanggan Yth. \n"
            var footerTeks = "Mohon pembayaran dilakukan melalui pembayaran resmi Pelayanan Gas Bumi Kota Depok selambat-lambatnya tanggal 20 setiap bulannya. " +
                    "Abaikan pesan ini jika sudah melakukan pembayaran. \n" +
                    "Terima kasih\n" +
                    "\n" +
                    "Pengelola Jargas Kota Depok \n" +
                    "*Contak Center : 0811-1977-774.*\n" +
                    "Dimohonkan untuk tidak mengirim pesan ke nomor ini"

            if (teksHeader != "" && teksBottom != ""){
                headerTeks = teksHeader.toString()
                footerTeks = teksBottom.toString()
            }

            val resultList = mutableListOf<String>()

            val lines = rincian.split("\n")
            var counter = 1
            for (line in lines) {
                val parts = line.split(":")
                if (parts.size == 3) {
                    val nam = parts[1].trim()
                    val value = parts[2].trim()
                    val formattedLine = "$counter. $nam\t: $value"
                    resultList.add(formattedLine)
                    counter++
                }
            }
            val msg = "Id Pelanggan : *$id* \n" +
                    "Nama Pelanggan : *$nama* \n" +
//                    "Alamat : *$alamat* \n" +
//                    "Sektor : *$sector* \n" +
//                    "Jumlah Tagihan: *$bulan Bulan* \n" +
                    "Rincian Tagihan:\n" +
                    "${resultList.joinToString("\n"){it->
                        "*$it*"
                    }} \n" +
                    "Total Biaya\t*: $total* \n"
            val client = SyncHttpClient()
            client.setTimeout(30000) // 30 Detik
//            val url = "http://116.203.191.58/api/send_message"
            val url = "https://api.watzap.id/v1/send_message"
            val jsonParams = JSONObject()
            jsonParams.put("phone_no", phone)
            jsonParams.put("api_key", "")
            jsonParams.put("message", headerTeks + msg + footerTeks)
            jsonParams.put("number_key", "")
//            jsonParams.put("flag_retry", "on")
//            jsonParams.put("pendingTime", 5)
            val entity = StringEntity(jsonParams.toString())

            client.post(this@TagihanActivity, url, entity, "application/json", object : AsyncHttpResponseHandler() {
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
            R.id.editPesan -> {
                val dialogView = layoutInflater.inflate(R.layout.edit_pesan_wa, null)
                val etHeader = dialogView.findViewById<EditText>(R.id.etTeksHeader)
                val etMid = dialogView.findViewById<EditText>(R.id.etTeksMid)
                val etBottom = dialogView.findViewById<EditText>(R.id.etTeksBottom)
                etBottom.setText("Mohon pembayaran dilakukan melalui pembayaran resmi Pelayanan Gas Bumi Kota Depok selambat-lambatnya tanggal 20 setiap bulannya. Abaikan pesan ini jika sudah melakukan pembayaran.\nTerima kasih\n\n Pengelola Jargas Kota Depok\n *Contak Center : 0811-1977-774.*\nDimohonkan untuk tidak reply pesan ini")
                val simpan = dialogView.findViewById<AppCompatButton>(R.id.btnSubmit)
                val teksHeader = pref.getString("HEADER","")
                val teksBottom = pref.getString("BOTTOM","")
                if (teksHeader != "" && teksBottom != ""){
                    etHeader.setText(teksHeader)
                    etBottom.setText(teksBottom)
                }
                simpan.setOnClickListener{
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Apakah anda mau simpan perubahan?")
                    builder.setPositiveButton("Ya"){_,_->
                        editor.putString("HEADER", etHeader.text.toString())
                        editor.putString("BOTTOM", etBottom.text.toString())
                        editor.commit()
                    }
                    builder.setNegativeButton("Tidak"){p,_->
                        p.dismiss()
                    }
                    builder.create().show()
                }
                bottomSheetDialog = BottomSheetDialog(this@TagihanActivity)
                bottomSheetDialog.setContentView(dialogView)
                bottomSheetDialog.show()
            }
            R.id.sendWaNoAcc -> {
                lifecycleScope.launch{
                    var i = 0
                    val result = withContext(Dispatchers.IO){
                        mTunggakan.forEach{ it ->
                            if (it.selesai != "ADA"){
                                i++
                            }
                        }
                        return@withContext i
                    }
                    val builder = AlertDialog.Builder(this@TagihanActivity)
                    builder.setTitle("Apakah anda yakin kirim pesan sebanyak $result")
                    builder.setPositiveButton("Ya"){_,_->
                        val progressBar = ProgressDialog(this@TagihanActivity)
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
                                    if (it.selesai != "ADA"){
                                        withContext(Dispatchers.Main) {
                                            progressBar.progress = terkirim
                                        }
                                        spanChat++
                                        delay(10000)
                                        val hasil = sendWa(it.phoneTunggakan.toString(), spanChat, it.idTunggakan.toString(), it.namaTunggakan.toString(), it.alamatTunggakan.toString(), it.sectorTunggakan.toString(), it.tagihanTunggakan.toString(), it.rincianTunggakan.toString(), formatRupiah().format(it.jumlahTunggakan).replace(",00", ""))
                                        Log.e("sudah terkirim", terkirim.toString())
                                        Log.e("hasil send Wa", hasil)
                                        Log.e("Erorr balasan", hasil)
//                                        Log.e("nomor send wa", it.phoneTunggakan.toString())
                                        terkirim++
                                        if (hasil == "${it.namaTunggakan} berhasil"){
                                            withContext(Dispatchers.IO){
                                                val contentValues = ContentValues()
                                                contentValues.put(DbContract.NoteColumns.REGNO, it.idTunggakan)
                                                dbQuery.insert(contentValues)
                                            }
                                        }else{
                                            withContext(Dispatchers.Main){
                                                arrayGagal.add(it.namaTunggakan.toString())
                                                Log.e("Erorr balasan", hasil)
                                            }
                                        }
                                        if (spanChat == 3){
                                            spanChat = 0
                                        }
                                    }
                                }
                            } finally {
                                withContext(Dispatchers.Main) {
                                    progressBar.dismiss()
                                    val builder = AlertDialog.Builder(this@TagihanActivity)
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
            R.id.sendWa -> {
                lifecycleScope.launch{
                    var i = 0
                    val result = withContext(Dispatchers.IO){
                        mTunggakan.forEach{ _ ->
                            i++
                        }
                        return@withContext i
                    }
                    val builder = AlertDialog.Builder(this@TagihanActivity)
                    builder.setTitle("Apakah anda yakin kirim pesan sebanyak $result")
                    builder.setPositiveButton("Ya"){_,_->
                        val progressBar = ProgressDialog(this@TagihanActivity)
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
                                    spanChat++
                                    delay(10000)
                                    val hasil = sendWa(it.phoneTunggakan.toString(), spanChat, it.idTunggakan.toString(), it.namaTunggakan.toString(), it.alamatTunggakan.toString(), it.sectorTunggakan.toString(), it.tagihanTunggakan.toString(), it.rincianTunggakan.toString(), formatRupiah().format(it.jumlahTunggakan).replace(",00", ""))
//                                    Log.e("sudah terkirim", terkirim.toString())
//                                    Log.e("hasil send Wa", hasil)
//                                    Log.e("nomor send wa", it.phoneTunggakan.toString())
                                    terkirim++
                                    if (hasil == "${it.namaTunggakan} berhasil"){
                                        withContext(Dispatchers.IO){
                                            val contentValues = ContentValues()
                                            contentValues.put(DbContract.NoteColumns.REGNO, it.idTunggakan)
                                            dbQuery.insert(contentValues)
                                        }
                                    }else{
                                        withContext(Dispatchers.Main){
                                            arrayGagal.add(it.namaTunggakan.toString())
                                            Log.e("Erorr balasan", hasil)
                                        }
                                    }
                                    if (spanChat == 3){
                                        spanChat = 0
                                    }
                                }
                            } finally {
                                withContext(Dispatchers.Main) {
                                    progressBar.dismiss()
                                    val builder = AlertDialog.Builder(this@TagihanActivity)
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
                    val builder = AlertDialog.Builder(this@TagihanActivity)
                    builder.setTitle("Apakah anda yakin kirim pesan sebanyak $result")
                    builder.setPositiveButton("Ya"){_,_->
                        val progressBar = ProgressDialog(this@TagihanActivity)
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
                                    spanChat++
                                    delay(10000)
                                    val hasil = sendWa(it.phoneTunggakan.toString(), spanChat, it.idTunggakan.toString(), it.namaTunggakan.toString(), it.alamatTunggakan.toString(), it.sectorTunggakan.toString(), it.tagihanTunggakan.toString(), it.rincianTunggakan.toString(), formatRupiah().format(it.jumlahTunggakan).replace(",00", ""))
                                    Log.e("hasil send Wa", hasil)
                                    terkirim++
                                    if (hasil == "${it.namaTunggakan} berhasil"){
                                        withContext(Dispatchers.IO){
                                            val contentValues = ContentValues()
                                            contentValues.put(DbContract.NoteColumns.REGNO, it.idTunggakan)
                                            dbQuery.insert(contentValues)
                                        }
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
                                    if (spanChat == 3){
                                        spanChat = 0
                                    }
                                }
                            } finally {
                                withContext(Dispatchers.Main) {
                                    progressBar.dismiss()
                                    val builder = AlertDialog.Builder(this@TagihanActivity)
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