package com.example.blastjargas.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.blastjargas.R
import com.example.blastjargas.model.Status

class ListStatusAdapter(private var sector: MutableList<Status>):RecyclerView.Adapter<ListStatusAdapter.ViewHolder>() {

    fun setOnItemClickCallback(onItemClickCallback: OnItemClickCallback) {
        this.onItemClickCallback = onItemClickCallback
    }

    interface OnItemClickCallback {
        fun onItemClicked(sector: Status)
    }

    private var onItemClickCallback: ListStatusAdapter.OnItemClickCallback? = null

    inner class ViewHolder(itemsView: View):RecyclerView.ViewHolder(itemsView) {
        private var tvSector = itemsView.findViewById<TextView>(R.id.tvListStatus)
        private var tvResultSector = itemsView.findViewById<TextView>(R.id.tvResultStatus)
        fun bind(sector_: Status){
            tvSector.text = sector_.status
            tvResultSector.text = sector_.semua.toString()
            itemView.setOnClickListener{
                onItemClickCallback?.onItemClicked(sector_)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_status,parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = sector.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(sector[position])
    }
}