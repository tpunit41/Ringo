package com.ringo.app

import android.content.ContentValues
import android.content.Intent
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class TrimActivity: AppCompatActivity() {
    private lateinit var uri:Uri
    private var duration=0L
    private var player:MediaPlayer?=null
    private lateinit var start:SeekBar; private lateinit var end:SeekBar
    override fun onCreate(b:Bundle?){
        super.onCreate(b); setContentView(R.layout.activity_trim)
        val id=intent.getLongExtra("id",-1); duration=intent.getLongExtra("duration",0)
        uri=Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,id.toString())
        findViewById<TextView>(R.id.title).text=intent.getStringExtra("title")?:"Song"
        start=findViewById(R.id.start); end=findViewById(R.id.end)
        start.max=duration.toInt().coerceAtLeast(1); end.max=start.max; end.progress=start.max.coerceAtLeast(1)
        start.setOnSeekBarChangeListener(listener()); end.setOnSeekBarChangeListener(listener())
        findViewById<Button>(R.id.preview).setOnClickListener{ preview() }
        findViewById<Button>(R.id.export).setOnClickListener{ export(0) }
        findViewById<Button>(R.id.notification).setOnClickListener{ export(1) }
        findViewById<Button>(R.id.alarm).setOnClickListener{ export(2) }
        update()
    }
    private fun listener()=object:SeekBar.OnSeekBarChangeListener{
        override fun onProgressChanged(s:SeekBar?,p:Int,f:Boolean){ if(start.progress>end.progress) end.progress=start.progress; update() }
        override fun onStartTrackingTouch(s:SeekBar?){}
        override fun onStopTrackingTouch(s:SeekBar?){}
    }
    private fun update(){findViewById<TextView>(R.id.time).text="Start: ${start.progress/1000}s   End: ${end.progress/1000}s   Length: ${(end.progress-start.progress)/1000}s"}
    private fun preview(){
        player?.release(); player=MediaPlayer.create(this,uri); player?.seekTo(start.progress)
        player?.setOnPreparedListener{it.start()}
        player?.setOnCompletionListener{it.release()}
    }
    private fun export(type:Int){
        val s=start.progress.toLong()*1000; val e=end.progress.toLong()*1000
        if(e<=s){toast("End time must be after start");return}
        val file=File(cacheDir,"ringo_${System.currentTimeMillis()}.m4a")
        try {
            trimWithExtractor(s,e,file)
            val name="Ringo_${intent.getStringExtra("title")?: "Tone"}_${System.currentTimeMillis()}.m4a"
            val values=ContentValues().apply{
                put(MediaStore.Audio.Media.DISPLAY_NAME,name); put(MediaStore.Audio.Media.MIME_TYPE,"audio/mp4")
                put(MediaStore.Audio.Media.IS_MUSIC, type==0); put(MediaStore.Audio.Media.IS_RINGTONE,type==0)
                put(MediaStore.Audio.Media.IS_NOTIFICATION,type==1); put(MediaStore.Audio.Media.IS_ALARM,type==2)
                put(MediaStore.Audio.Media.RELATIVE_PATH,Environment.DIRECTORY_RINGTONES)
                put(MediaStore.Audio.Media.TITLE,name.removeSuffix(".m4a"))
            }
            val outUri=contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,values)
                ?: throw Exception("Could not create media entry")
            contentResolver.openOutputStream(outUri)!!.use{dest->FileInputStream(file).use{src->src.copyTo(dest)}}
            file.delete()
            if(!Settings.System.canWrite(this)){
                startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))
                toast("Allow 'Modify system settings', then return to Ringo and set the tone.")
            } else {
                android.media.RingtoneManager.setActualDefaultRingtoneUri(this,
                    when(type){1->android.media.RingtoneManager.TYPE_NOTIFICATION;2->android.media.RingtoneManager.TYPE_ALARM;else->android.media.RingtoneManager.TYPE_RINGTONE},outUri)
                toast("Tone set successfully")
            }
        }catch(ex:Exception){toast("Export failed: ${ex.message}")}
    }
    private fun trimWithExtractor(startUs:Long,endUs:Long,file:File){
        val ex=MediaExtractor(); ex.setDataSource(this,uri,null)
        var track=-1
        for(i in 0 until ex.trackCount){val f=ex.getTrackFormat(i); if(f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/")==true){track=i;break}}
        if(track<0)throw Exception("Audio track not found")
        ex.selectTrack(track)
        val format=ex.getTrackFormat(track)
        val mux=MediaMuxer(file.absolutePath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val mt=mux.addTrack(format); mux.start()
        ex.seekTo(startUs,MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        val buf=java.nio.ByteBuffer.allocate(1024*1024); val info=android.media.MediaCodec.BufferInfo()
        while(true){
            val ts=ex.sampleTime; if(ts<0 || ts>endUs)break
            info.offset=0; info.size=ex.readSampleData(buf,0); info.presentationTimeUs=ts-startUs
            info.flags=ex.sampleFlags; if(info.size<0)break
            mux.writeSampleData(mt,buf,info); ex.advance()
        }
        mux.stop(); mux.release(); ex.release()
    }
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_LONG).show()
    override fun onDestroy(){player?.release();super.onDestroy()}
}
