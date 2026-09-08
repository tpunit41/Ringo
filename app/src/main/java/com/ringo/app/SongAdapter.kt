package com.ringo.app

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SongAdapter(private var songs: List<Song>, private val click: (Song) -> Unit) :
    RecyclerView.Adapter<SongAdapter.VH>() {
    class VH(val t: TextView): RecyclerView.ViewHolder(t)
    override fun onCreateViewHolder(p: ViewGroup, v: Int): VH {
        val t = TextView(p.context).apply {
            setTextColor(0xFFFFFFFF.toInt()); textSize=17f; setPadding(16,22,16,22)
        }
        return VH(t)
    }
    override fun getItemCount() = songs.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val s=songs[pos]; h.t.text="${s.title}\n${s.artist}"
        h.t.setOnClickListener { click(s) }
    }
    fun update(x: List<Song>) { songs=x; notifyDataSetChanged() }
}
