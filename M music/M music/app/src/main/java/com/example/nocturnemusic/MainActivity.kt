package com.example.nocturnemusic

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

data class Song(val id: Long, val title: String, val artist: String, val uri: Uri)

class MainActivity : AppCompatActivity() {
    private lateinit var list: LinearLayout
    private lateinit var nowTitle: TextView
    private lateinit var nowArtist: TextView
    private lateinit var playPause: Button
    private var songs: List<Song> = emptyList()
    private var player: MediaPlayer? = null
    private var currentIndex = -1
    private var playing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(11, 13, 12)
        window.navigationBarColor = android.graphics.Color.rgb(11, 13, 12)
        buildUi()
        requestAudioPermission()
    }

    private fun buildUi() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(12))
            setBackgroundColor(0xFF0B0D0C.toInt())
        }
        val brand = text("NOCTURNE", 13, 0xFFB7F36B.toInt(), true)
        brand.letterSpacing = .18f
        page.addView(brand)
        page.addView(text("Your music.\\nYour atmosphere.", 30, 0xFFF4F5F1.toInt(), true).apply {
            setPadding(0, dp(12), 0, dp(18))
        })
        page.addView(text("ON THIS DEVICE", 11, 0xFF8B918B.toInt(), true).apply {
            letterSpacing = .12f
            setPadding(0, 0, 0, dp(10))
        })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = android.widget.ScrollView(this).apply { addView(list) }
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val divider = View(this).apply { setBackgroundColor(0xFF2A2E2A.toInt()) }
        page.addView(divider, LinearLayout.LayoutParams(-1, dp(1)))
        nowTitle = text("Nothing playing", 16, 0xFFF4F5F1.toInt(), true)
        nowArtist = text("Choose a track from your library", 12, 0xFF9DA39C.toInt(), false)
        page.addView(nowTitle)
        page.addView(nowArtist)
        val controls = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
        }
        val previous = button("⏮")
        playPause = button("▶")
        val next = button("⏭")
        val aod = button("AOD")
        controls.addView(previous, weightParams())
        controls.addView(playPause, weightParams())
        controls.addView(next, weightParams())
        controls.addView(aod, weightParams())
        page.addView(controls)
        previous.setOnClickListener { playAt((currentIndex - 1).coerceAtLeast(0)) }
        next.setOnClickListener { if (songs.isNotEmpty()) playAt((currentIndex + 1) % songs.size) }
        playPause.setOnClickListener { togglePlayback() }
        aod.setOnClickListener { startActivity(android.content.Intent(this, AmbientActivity::class.java).apply {
            putExtra("title", nowTitle.text.toString())
            putExtra("artist", nowArtist.text.toString())
        }) }
        setContentView(page)
    }

    private fun requestAudioPermission() {
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
            else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadSongs()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 10)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) loadSongs()
        else Toast.makeText(this, "Allow audio access to show music stored on your phone.", Toast.LENGTH_LONG).show()
    }

    private fun loadSongs() {
        val result = mutableListOf<Song>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        contentResolver.query(collection, projection, selection, null, "${MediaStore.Audio.Media.TITLE} ASC")?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                result.add(Song(id, c.getString(titleCol) ?: "Unknown title",
                    c.getString(artistCol) ?: "Unknown artist", ContentUris.withAppendedId(collection, id)))
            }
        }
        songs = result
        list.removeAllViews()
        if (songs.isEmpty()) {
            list.addView(text("No local tracks found yet. Add audio files to your phone and refresh.", 14, 0xFF9DA39C.toInt(), false).apply {
                setPadding(0, dp(14), 0, dp(14))
            })
        } else songs.forEachIndexed { index, song ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(2), dp(12), dp(2), dp(12))
                isClickable = true
                isFocusable = true
                setOnClickListener { playAt(index) }
            }
            row.addView(text(song.title, 15, 0xFFF4F5F1.toInt(), true))
            row.addView(text(song.artist, 12, 0xFF9DA39C.toInt(), false).apply { setPadding(0, dp(4), 0, 0) })
            list.addView(row)
            list.addView(View(this).apply { setBackgroundColor(0xFF222622.toInt()) }, LinearLayout.LayoutParams(-1, dp(1)))
        }
    }

    private fun playAt(index: Int) {
        if (index !in songs.indices) return
        try {
            player?.release()
            currentIndex = index
            val song = songs[index]
            player = MediaPlayer().apply {
                setDataSource(this@MainActivity, song.uri)
                setOnCompletionListener { if (songs.isNotEmpty()) playAt((currentIndex + 1) % songs.size) }
                prepare()
                start()
            }
            playing = true
            nowTitle.text = song.title
            nowArtist.text = song.artist
            playPause.text = "Ⅱ"
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't play this audio file.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun togglePlayback() {
        val p = player
        if (p == null) {
            if (songs.isNotEmpty()) playAt(0)
            return
        }
        if (playing) { p.pause(); playing = false; playPause.text = "▶" }
        else { p.start(); playing = true; playPause.text = "Ⅱ" }
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean) = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }
    private fun button(label: String) = Button(this).apply {
        text = label; isAllCaps = false; setTextColor(0xFFB7F36B.toInt())
        backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF1B211A.toInt())
    }
    private fun weightParams() = LinearLayout.LayoutParams(0, dp(48), 1f).apply { setMargins(dp(3), 0, dp(3), 0) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}
