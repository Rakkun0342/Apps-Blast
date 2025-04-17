package com.example.blastjargas.adapter

import android.app.Activity
import android.app.Notification.Action
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.marginTop
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.blastjargas.R
import com.example.blastjargas.model.MainViewModel
import com.example.blastjargas.model.Tunggakan
import java.text.NumberFormat
import java.util.Locale


class TunggakanAdapter(var tunggakan: MutableList<Tunggakan>):RecyclerView.Adapter<TunggakanAdapter.ViewHolder>(), Filterable {

    private val searchItem = ArrayList<Tunggakan>(tunggakan)
    private var onItemClickCallback: TunggakanAdapter.OnItemClickCallback? = null

    var activity: Activity? = null
    var arrayList: ArrayList<String>? = null
    var mainViewModel: MainViewModel? = null
    var isEnable = false
    var isSelectAll = false
    var selectList = ArrayList<String>()

    fun MainAdapter(activity: Activity, arrayList: ArrayList<String>) {
        this.activity = activity
        this.arrayList = arrayList
    }

    fun setOnItemClickCallback(onItemClickCallback: TunggakanAdapter.OnItemClickCallback) {
        this.onItemClickCallback = onItemClickCallback
    }

    interface OnItemClickCallback {
        fun onItemClicked(items: Tunggakan, position: Int)
    }

    inner class ViewHolder(itemView: View):RecyclerView.ViewHolder(itemView) {
        val idPelanggan: TextView = itemView.findViewById(R.id.tvTagihanId)
        val meter:TextView = itemView.findViewById(R.id.tvTunggakanMeter)
        val nama: TextView = itemView.findViewById(R.id.tvTunggakanNama)
        val alamat: TextView = itemView.findViewById(R.id.tvAlamatTunggakan)
        val phone: TextView = itemView.findViewById(R.id.tvNomor)
        val sector: TextView = itemView.findViewById(R.id.tvSectorTunggakan)
        val totalTunggakan: TextView = itemView.findViewById(R.id.totalTunggakan)
        val biaya: TextView = itemView.findViewById(R.id.tvTunggakanTotal)
        val rlSelesai: RelativeLayout = itemView.findViewById(R.id.rlSelesai)
        val rlDitandai: RelativeLayout = itemView.findViewById(R.id.rlTandai)
        val llLayoutTop : LinearLayout = itemView.findViewById(R.id.llLayoutTop)
        fun bind(mTunggakan: Tunggakan, position: Int){
            if (mTunggakan.biayaTunggakan != null){
                rlSelesai.visibility = View.GONE
                if (mTunggakan.selesai == "ADA"){
                    rlSelesai.visibility = View.VISIBLE
                }
                rlDitandai.visibility = View.GONE
                if (mTunggakan.selesai == "TANDA"){
                    rlDitandai.visibility = View.VISIBLE
                }
                idPelanggan.text = mTunggakan.idTunggakan.toString()
                meter.text = mTunggakan.noMeter.toString()
                nama.text = mTunggakan.namaTunggakan
                alamat.text = mTunggakan.alamatTunggakan
                sector.text = mTunggakan.sectorTunggakan
                phone.text = mTunggakan.phoneTunggakan
                totalTunggakan.text = NumberFormat.getIntegerInstance().format(mTunggakan.tagihanTunggakan)
                biaya.text = NumberFormat.getIntegerInstance().format(mTunggakan.jumlahTunggakan)
                itemView.setOnClickListener { onItemClickCallback?.onItemClicked(mTunggakan, position) }
            }else{
                rlSelesai.visibility = View.GONE
                if (mTunggakan.selesai == "ADA"){
                    rlSelesai.visibility = View.VISIBLE
                }
                rlDitandai.visibility = View.GONE
                if (mTunggakan.selesai == "TANDA"){
                    val layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT)
                    layoutParams.topMargin = 20
                    llLayoutTop.layoutParams = layoutParams
                    rlDitandai.visibility = View.VISIBLE
                }

                idPelanggan.text = mTunggakan.idTunggakan.toString()
                meter.text = mTunggakan.noMeter.toString()
                nama.text = mTunggakan.namaTunggakan
                alamat.text = mTunggakan.alamatTunggakan
                sector.text = mTunggakan.sectorTunggakan
                phone.text = mTunggakan.phoneTunggakan
                totalTunggakan.visibility = View.GONE
                biaya.visibility = View.GONE
                itemView.setOnClickListener { onItemClickCallback?.onItemClicked(mTunggakan, position) }
            }

            /*itemView.setOnLongClickListener(object : View.OnLongClickListener{
                override fun onLongClick(v: View): Boolean {
                    if (!isEnable){
                        callback=new ActionMode.Callback() {
                            @Override
                            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                                // initialize menu inflater
                                MenuInflater menuInflater= mode.getMenuInflater();
                                // inflate menu
                                menuInflater.inflate(R.menu.menu,menu);
                                // return true
                                return true;
                            }

                            @Override
                            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                                // when action mode is prepare
                                // set isEnable true
                                isEnable=true;
                                // create method
                                ClickItem(holder);
                                // set observer on getText method
                                mainViewModel.getText().observe((LifecycleOwner) activity
                                    , new Observer<String>() {
                                        @Override
                                        public void onChanged(String s) {
                                            // when text change
                                            // set text on action mode title
                                            mode.setTitle(String.format("%s Selected",s));
                                        }
                                    });
                                // return true
                                return true;
                            }

                            @Override
                            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                                // when click on action mode item
                                // get item  id
                                int id=item.getItemId();
                                // use switch condition
                                switch(id)
                                {
                                    case R.id.menu_delete:
                                    // when click on delete
                                    // use for loop
                                    for(String s:selectList)
                                    {
                                        // remove selected item list
                                        arrayList.remove(s);
                                    }
                                    // check condition
                                    if(arrayList.size()==0)
                                    {
                                        // when array list is empty
                                        // visible text view
                                        tvEmpty.setVisibility(View.VISIBLE);
                                    }
                                    // finish action mode
                                    mode.finish();
                                    break;

                                    case R.id.menu_select_all:
                                    // when click on select all
                                    // check condition
                                    if(selectList.size()==arrayList.size())
                                    {
                                        // when all item selected
                                        // set isselectall false
                                        isSelectAll=false;
                                        // create select array list
                                        selectList.clear();
                                    }
                                    else
                                    {
                                        // when  all item unselected
                                        // set isSelectALL true
                                        isSelectAll=true;
                                        // clear select array list
                                        selectList.clear();
                                        // add value in select array list
                                        selectList.addAll(arrayList);
                                    }
                                    // set text on view model
                                    mainViewModel.setText(String .valueOf(selectList.size()));
                                    // notify adapter
                                    notifyDataSetChanged();
                                    break;
                                }
                                // return true
                                return true;
                            }
                        }
                }

            })*/
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_tunggakan,parent,false)
//        mainViewModel = ViewModelProvider(activity as FragmentActivity)[MainViewModel::class.java]
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = tunggakan.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(tunggakan[position], position)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(p0: CharSequence?): FilterResults {
                val filteredList = ArrayList<Tunggakan>()

                if (p0!!.isBlank() or p0.isEmpty()){
                    filteredList.addAll(searchItem)
                }else{
                    val filterPatern = p0.toString().toLowerCase(Locale.ROOT).trim()

                    searchItem.forEach{
                        if (it.idTunggakan!!.toString().toLowerCase(Locale.ROOT).contains(filterPatern)){
                            filteredList.add(it)
                        }
                        if (it.noMeter!!.toString().toLowerCase(Locale.ROOT).contains(filterPatern)){
                            filteredList.add(it)
                        }
                        if (it.namaTunggakan!!.toLowerCase(Locale.ROOT).contains(filterPatern)){
                            filteredList.add(it)
                        }
                    }
                }

                val result = FilterResults()
                result.values = filteredList
                return result
            }

            override fun publishResults(p0: CharSequence, p1: FilterResults) {
                tunggakan.clear()
                tunggakan.addAll(p1.values as List<Tunggakan>)
                notifyDataSetChanged()
            }
        }
    }
}