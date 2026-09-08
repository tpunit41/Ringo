package com.ringo.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var adapter: SongAdapter
    private var songs = listOf<Song>()
    override fun onCreate(b: Bundle?) {
        super.onCreate(b); setContentView(R.layout.activity_main)
        adapter=SongAdapter(emptyList()) { s ->
            startActivity(Intent(this, TrimActivity::class.java).apply {
                putExtra("id",s.id); putExtra("title",s.title); putExtra("duration",s.duration)
            })
        }
        findViewById<RecyclerView>(R.id.list).layoutManager=LinearLayoutManager(this)
        findViewById<RecyclerView>(R.id.list).adapter=adapter
        val q=findViewById<EditText>(R.id.search)
        q.addTextChangedListener(object: android.text.TextWatcher{
            override fun beforeTextChanged(s:CharSequence?,st:Int,c:Int,a:Int){}
            override fun onTextChanged(s:CharSequence?,st:Int,b:Int,c:Int){ filter(s?.toString().orEmpty()) }
            override fun afterTextChanged(e:android.text.Editable?){}
        })
        requestAudio()
    }
    private fun requestAudio(){
        val p=if(android.os.Build.VERSION.SDK_INT>=33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        if(ActivityCompat.checkSelfPermission(this,p)!=PackageManager.PERMISSION_GRANTED) ActivityCompat.requestPermissions(this,arrayOf(p),10)
        else load()
    }
    override fun onRequestPermissionsResult(r:Int,p:Array<String>,g:IntArray){super.onRequestPermissionsResult(r,p,g);if(r==10 && g.firstOrNull()==PackageManager.PERMISSION_GRANTED)load()}
    private fun load(){
        val out=mutableListOf<Song>()
        contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Media._ID,MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.ARTIST,MediaStore.Audio.Media.DURATION),
            "${MediaStore.Audio.Media.IS_MUSIC}=1",null,MediaStore.Audio.Media.TITLE+" COLLATE NOCASE ASC")?.use{ c->
            val i=c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID); val t=c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val a=c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST); val d=c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while(c.moveToNext()) out.add(Song(c.getLong(i),c.getString(t),c.getString(a),c.getLong(d)))
        }
        songs=out; adapter.update(out)
    }
    private fun filter(q:String){adapter.update(songs.filter{it.title.contains(q,true)||it.artist.contains(q,true)})}
}
