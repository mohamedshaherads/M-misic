package com.mmusic.player

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

private val Ink = Color(0xFF070A09)
private val Panel = Color(0xFF111715)
private val Mint = Color(0xFF69E6BB)
private val Muted = Color(0xFF9AA7A2)

data class Song(val id: Long, val title: String, val artist: String, val album: String, val uri: Uri)

class MainActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer
    private var songs by mutableStateOf<List<Song>>(emptyList())
    private var permissionGranted by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted) loadSongs()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
                         else Manifest.permission.READ_EXTERNAL_STORAGE
        permissionGranted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        if (permissionGranted) loadSongs() else permissionLauncher.launch(permission)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Ink, surface = Panel, primary = Mint,
                    onBackground = Color.White, onSurface = Color.White
                )
            ) {
                MusicHome(
                    songs = songs,
                    player = player,
                    permissionGranted = permissionGranted,
                    onRequestPermission = { permissionLauncher.launch(permission) }
                )
            }
        }
    }

    private fun loadSongs() {
        val found = mutableListOf<Song>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sort = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        contentResolver.query(collection, projection, selection, null, sort)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                found += Song(
                    id,
                    cursor.getString(titleCol) ?: "Unknown title",
                    cursor.getString(artistCol)?.takeUnless { it == "<unknown>" } ?: "Unknown artist",
                    cursor.getString(albumCol) ?: "Unknown album",
                    ContentUris.withAppendedId(collection, id)
                )
            }
        }
        songs = found
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }
}

@Composable
private fun MusicHome(
    songs: List<Song>,
    player: ExoPlayer,
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Songs") }
    var currentSong by remember { mutableStateOf<Song?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentSong = songs.firstOrNull { it.uri.toString() == mediaItem?.localConfiguration?.uri.toString() }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    Column(Modifier.fillMaxSize().background(Ink).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 18.dp, top = 18.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("M Music", fontSize = 29.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("YOUR SOUND. YOUR WAY.", fontSize = 10.sp, letterSpacing = 2.sp, color = Mint)
            }
            IconButton(onClick = { selectedTab = "Search" }) {
                Icon(Icons.Default.Search, "Search", tint = Color.White)
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Songs", "Albums", "Artists").forEach { tab ->
                Surface(
                    color = if (selectedTab == tab) Mint else Panel,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.clickable { selectedTab = tab }
                ) {
                    Text(tab, Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        color = if (selectedTab == tab) Ink else Muted, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (!permissionGranted) {
            Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.LibraryMusic, null, tint = Mint, modifier = Modifier.size(54.dp))
                Spacer(Modifier.height(14.dp))
                Text("Your music, in one place", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Allow audio access to scan music stored on this phone.", color = Muted)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRequestPermission) { Text("Allow access") }
            }
        } else if (songs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.MusicNote, null, tint = Mint, modifier = Modifier.size(54.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No music found", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("Add audio files to your phone and reopen M Music.", color = Muted)
                }
            }
        } else {
            Text(
                if (selectedTab == "Songs") "YOUR LIBRARY  ·  ${songs.size} TRACKS" else "YOUR LIBRARY",
                color = Muted, fontSize = 11.sp, letterSpacing = 1.5.sp,
                modifier = Modifier.padding(start = 22.dp, top = 22.dp, bottom = 8.dp)
            )
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp)) {
                itemsIndexed(songs) { index, song ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable {
                                currentSong = song
                                val items = songs.map { MediaItem.fromUri(it.uri) }
                                player.setMediaItems(items, index, 0L)
                                player.prepare()
                                player.play()
                            }
                            .padding(horizontal = 18.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Panel),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentSong?.id == song.id && isPlaying) Icon(Icons.Default.Equalizer, null, tint = Mint)
                            else Text("%02d".format(index + 1), color = Muted, fontSize = 12.sp)
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = if (currentSong?.id == song.id) Mint else Color.White, fontWeight = FontWeight.Medium)
                            Text("${song.artist}  ·  ${song.album}", maxLines = 1,
                                overflow = TextOverflow.Ellipsis, color = Muted, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.MoreHoriz, null, tint = Muted)
                    }
                }
            }
        }

        currentSong?.let { song ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(18.dp)).background(Panel).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF17372D)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MusicNote, null, tint = Mint)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                    Text(song.artist, color = Muted, fontSize = 12.sp)
                }
                IconButton(onClick = {
                    if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
                }) { Icon(Icons.Default.SkipPrevious, null, tint = Color.White) }
                IconButton(onClick = { if (player.isPlaying) player.pause() else player.play() }) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Mint)
                }
                IconButton(onClick = {
                    if (player.hasNextMediaItem()) player.seekToNextMediaItem()
                }) { Icon(Icons.Default.SkipNext, null, tint = Color.White) }
            }
        }

        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 8.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(Icons.Default.Home to "Home", Icons.Default.Search to "Search", Icons.Default.LibraryMusic to "Library").forEach { (icon, label) ->
                Column(Modifier.clickable { selectedTab = if (label == "Search") "Search" else "Songs" },
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(icon, null, tint = if ((label == "Search" && selectedTab == "Search") || (label != "Search" && selectedTab != "Search")) Mint else Muted)
                    Text(label, fontSize = 10.sp, color = Muted)
                }
            }
        }
    }
}
