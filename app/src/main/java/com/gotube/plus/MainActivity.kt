package com.gotube.plus

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.google.common.util.concurrent.ListenableFuture
import java.util.Locale

private val AccentRed = Color(0xFFE53935)
private val DarkBackground = Color(0xFF101010)
private val DarkSurface = Color(0xFF1B1B1B)

class MainActivity : ComponentActivity() {
    private lateinit var libraryStore: LibraryStore
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController by mutableStateOf<MediaController?>(null)

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        libraryStore = LibraryStore(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        val token = SessionToken(this, android.content.ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener({
            runCatching { future.get() }
                .onSuccess { mediaController = it }
                .onFailure { Toast.makeText(this, "Pemutar belum dapat dimulai.", Toast.LENGTH_SHORT).show() }
        }, ContextCompat.getMainExecutor(this))

        setContent {
            var darkMode by remember { mutableStateOf(libraryStore.isDarkMode()) }
            GoTubeTheme(darkMode = darkMode) {
                GoTubeApp(
                    store = libraryStore,
                    controller = mediaController,
                    darkMode = darkMode,
                    onDarkModeChange = {
                        darkMode = it
                        libraryStore.setDarkMode(it)
                    },
                    onEnterPictureInPicture = ::enterPictureInPicture
                )
            }
        }
    }

    private fun enterPictureInPicture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setAspectRatio(android.util.Rational(16, 9))
                    .build()
            )
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (libraryStore.pictureInPictureEnabled() && mediaController?.isPlaying == true && Build.VERSION.SDK_INT >= 26) {
            enterPictureInPicture()
        }
    }

    override fun onDestroy() {
        controllerFuture?.let(MediaController::releaseFuture)
        mediaController = null
        super.onDestroy()
    }
}

@Composable
private fun GoTubeTheme(darkMode: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkMode) {
        androidx.compose.material3.darkColorScheme(
            primary = AccentRed,
            onPrimary = Color.White,
            background = DarkBackground,
            surface = DarkSurface,
            onBackground = Color(0xFFF4F4F4),
            onSurface = Color(0xFFF4F4F4)
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = Color(0xFFC62828),
            onPrimary = Color.White,
            background = Color(0xFFF7F7F7),
            surface = Color.White,
            onBackground = Color(0xFF171717),
            onSurface = Color(0xFF171717)
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}

private enum class AppTab(val label: String) {
    HOME("Beranda"), SEARCH("Cari"), LIBRARY("Koleksi"), SETTINGS("Setelan")
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
private fun GoTubeApp(
    store: LibraryStore,
    controller: MediaController?,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onEnterPictureInPicture: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    var activeVideoId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPlaybackId by remember { mutableStateOf<String?>(null) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var favorites by remember { mutableStateOf(store.favorites()) }
    var history by remember { mutableStateOf(store.history()) }
    val context = LocalContext.current

    LaunchedEffect(controller, pendingPlaybackId) {
        val id = pendingPlaybackId ?: return@LaunchedEffect
        val player = controller ?: return@LaunchedEffect
        val index = VideoCatalog.items.indexOfFirst { it.id == id }
        if (index >= 0) {
            player.setMediaItems(VideoCatalog.items.map(VideoItem::toMediaItem), index, 0L)
            player.prepare()
            player.play()
            pendingPlaybackId = null
        }
    }

    BackHandler(enabled = activeVideoId != null) { activeVideoId = null }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (activeVideoId == null) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    AppTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = when (tab) {
                                        AppTab.HOME -> Icons.Default.Home
                                        AppTab.SEARCH -> Icons.Default.Search
                                        AppTab.LIBRARY -> Icons.Default.Favorite
                                        AppTab.SETTINGS -> Icons.Default.Settings
                                    },
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (activeVideoId != null) {
            val video = VideoCatalog.find(activeVideoId)
            if (video != null) {
                PlayerScreen(
                    video = video,
                    controller = controller,
                    onBack = { activeVideoId = null },
                    onNext = { controller?.seekToNextMediaItem() },
                    onFullscreen = { toggleFullscreen(context) },
                    onPip = onEnterPictureInPicture
                )
            }
        } else {
            when (selectedTab) {
                AppTab.HOME -> HomeScreen(
                    padding = padding,
                    store = store,
                    favorites = favorites,
                    history = history,
                    searchText = searchText,
                    onSearchTextChange = { searchText = it },
                    onSearch = { selectedTab = AppTab.SEARCH },
                    onPlay = { video ->
                        history = listOf(video.id) + history.filterNot { it == video.id }
                        store.addToHistory(video.id)
                        activeVideoId = video.id
                        pendingPlaybackId = video.id
                    },
                    onFavorite = { favorites = store.toggleFavorite(it) }
                )
                AppTab.SEARCH -> SearchScreen(
                    padding = padding,
                    query = searchText,
                    onQueryChange = { searchText = it },
                    onPlay = { video ->
                        history = listOf(video.id) + history.filterNot { it == video.id }
                        store.addToHistory(video.id)
                        activeVideoId = video.id
                        pendingPlaybackId = video.id
                    },
                    onFavorite = { favorites = store.toggleFavorite(it) },
                    favorites = favorites
                )
                AppTab.LIBRARY -> LibraryScreen(
                    padding = padding,
                    favorites = favorites,
                    history = history,
                    onPlay = { video ->
                        history = listOf(video.id) + history.filterNot { it == video.id }
                        store.addToHistory(video.id)
                        activeVideoId = video.id
                        pendingPlaybackId = video.id
                    },
                    onFavorite = { favorites = store.toggleFavorite(it) }
                )
                AppTab.SETTINGS -> SettingsScreen(
                    padding = padding,
                    darkMode = darkMode,
                    onDarkModeChange = onDarkModeChange,
                    pipEnabled = store.pictureInPictureEnabled(),
                    onPipChange = store::setPictureInPictureEnabled
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    padding: PaddingValues,
    store: LibraryStore,
    favorites: Set<String>,
    history: List<String>,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onSearch: () -> Unit,
    onPlay: (VideoItem) -> Unit,
    onFavorite: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("GOTUBE", color = AccentRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("PLUS", color = MaterialTheme.colorScheme.onBackground, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                }
                IconButton(onClick = onSearch) {
                    Icon(Icons.Default.Search, contentDescription = "Cari video", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
        item {
            SearchField(value = searchText, onValueChange = onSearchTextChange, onSubmit = onSearch, showMic = false)
        }
        if (history.isNotEmpty()) {
            item {
                SectionTitle("Lanjutkan menonton", "Riwayat terbaru")
                VideoCatalog.find(history.first())?.let { video ->
                    VideoCard(video, isFavorite = favorites.contains(video.id), onPlay = { onPlay(video) }, onFavorite = { onFavorite(video.id) })
                }
            }
        }
        item { SectionTitle("Pilihan untukmu", "Video demo dari sumber media terbuka") }
        items(VideoCatalog.items, key = VideoItem::id) { video ->
            VideoCard(video, favorites.contains(video.id), { onPlay(video) }, { onFavorite(video.id) })
        }
    }
}

@Composable
private fun SearchScreen(
    padding: PaddingValues,
    query: String,
    onQueryChange: (String) -> Unit,
    onPlay: (VideoItem) -> Unit,
    onFavorite: (String) -> Unit,
    favorites: Set<String>
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val recognized = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!recognized.isNullOrBlank()) onQueryChange(recognized)
    }
    val results = remember(query) {
        val terms = query.trim().lowercase(Locale("id", "ID"))
        if (terms.isBlank()) VideoCatalog.items else VideoCatalog.items.filter {
            "${it.title} ${it.creator} ${it.description}".lowercase(Locale("id", "ID")).contains(terms)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Cari video", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        SearchField(
            value = query,
            onValueChange = onQueryChange,
            onSubmit = { keyboard?.hide() },
            showMic = true,
            onMic = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Ucapkan kata yang ingin dicari")
                }
                runCatching { launcher.launch(intent) }.onFailure {
                    Toast.makeText(context, "Pencarian suara tidak tersedia di perangkat ini.", Toast.LENGTH_SHORT).show()
                }
            }
        )
        Text("${results.size} hasil", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f), fontSize = 13.sp)
        if (results.isEmpty()) {
            EmptyState("Tidak ada video yang cocok.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(results, key = VideoItem::id) { video ->
                    VideoCard(video, favorites.contains(video.id), { onPlay(video) }, { onFavorite(video.id) })
                }
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    padding: PaddingValues,
    favorites: Set<String>,
    history: List<String>,
    onPlay: (VideoItem) -> Unit,
    onFavorite: (String) -> Unit
) {
    var showHistory by rememberSaveable { mutableStateOf(false) }
    val videos = if (showHistory) history.mapNotNull(VideoCatalog::find) else VideoCatalog.items.filter { it.id in favorites }
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp, vertical = 16.dp)) {
        Text("Koleksi", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CollectionTab("Favorit", selected = !showHistory) { showHistory = false }
            CollectionTab("Riwayat", selected = showHistory) { showHistory = true }
        }
        Spacer(Modifier.height(16.dp))
        if (videos.isEmpty()) {
            EmptyState(if (showHistory) "Riwayat tontonan masih kosong." else "Video favorit akan muncul di sini.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(videos, key = VideoItem::id) { video ->
                    VideoCard(video, video.id in favorites, { onPlay(video) }, { onFavorite(video.id) })
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    padding: PaddingValues,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    pipEnabled: Boolean,
    onPipChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("Setelan", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        SettingRow("Mode gelap", "Tampilan hitam dengan aksen merah", darkMode, onDarkModeChange)
        SettingRow("Picture-in-Picture", "Jendela mini saat membuka aplikasi lain", pipEnabled, onPipChange)
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        Text("Tentang pemutaran", fontWeight = FontWeight.SemiBold)
        Text(
            "GoTube Plus memutar sumber media langsung yang mengizinkan pemutaran. Katalog saat ini berisi video demo; pencarian YouTube dan kualitas adaptif memerlukan integrasi resmi dan sumber yang mendukungnya.",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            fontSize = 14.sp,
            lineHeight = 21.sp
        )
        Text("Aplikasi ini tidak menyertakan iklan atau SDK iklan.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), fontSize = 14.sp)
        Text("Versi 1.0.0", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontSize = 12.sp)
    }
}

@Composable
@UnstableApi
private fun PlayerScreen(
    video: VideoItem,
    controller: MediaController?,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onFullscreen: () -> Unit,
    onPip: () -> Unit
) {
    val context = LocalContext.current
    var playing by remember { mutableStateOf(controller?.isPlaying == true) }
    var volume by remember { mutableFloatStateOf(controller?.volume ?: 1f) }

    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onVolumeChanged(volumeValue: Float) { volume = volumeValue }
        }
        controller?.addListener(listener)
        onDispose { controller?.removeListener(listener) }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Kembali") }
            Text(video.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                TextButton(onClick = onPip) { Text("PiP") }
            }
            TextButton(onClick = onFullscreen) { Text("Layar penuh") }
        }
        AndroidView(
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    player = controller
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            update = { it.player = controller }
        )
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(video.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(video.creator, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 14.sp)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { if (playing) controller?.pause() else controller?.play() }) {
                    Text(if (playing) "Jeda" else "Putar", color = AccentRed)
                }
                Text("Volume", fontSize = 13.sp)
                Slider(
                    value = volume,
                    onValueChange = { volume = it; controller?.volume = it },
                    modifier = Modifier.weight(1f),
                    valueRange = 0f..1f
                )
                TextButton(onClick = onNext) { Text("Berikutnya") }
            }
            Spacer(Modifier.height(6.dp))
            Text("Kualitas mengikuti file sumber.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f))
            Spacer(Modifier.height(14.dp))
            Text(video.description, fontSize = 14.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(14.dp))
            Text("Audio dapat tetap diputar di latar belakang melalui notifikasi media.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f))
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    showMic: Boolean,
    onMic: (() -> Unit)? = null
) {
    val keyboard = LocalSoftwareKeyboardController.current
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface)
            .padding(start = 14.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f).padding(vertical = 11.dp),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit(); keyboard?.hide() }),
            decorationBox = { inner ->
                if (value.isEmpty()) Text("Cari video", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f), fontSize = 15.sp)
                inner()
            }
        )
        if (showMic) {
            TextButton(onClick = { onMic?.invoke() }) {
                Text("Mic", color = AccentRed)
            }
        }
    }
}

@Composable
private fun VideoCard(video: VideoItem, isFavorite: Boolean, onPlay: () -> Unit, onFavorite: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onPlay),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(width = 132.dp, height = 82.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF292929))) {
                AsyncImage(model = video.thumbnailUrl, contentDescription = video.title, modifier = Modifier.fillMaxSize())
                Text(
                    video.durationLabel,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).clip(RoundedCornerShape(3.dp))
                        .background(Color.Black.copy(alpha = 0.8f)).padding(horizontal = 5.dp, vertical = 2.dp),
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(video.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text(video.creator, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f), fontSize = 12.sp, maxLines = 1)
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Hapus dari favorit" else "Tambah ke favorit",
                    tint = if (isFavorite) AccentRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(top = 2.dp, bottom = 1.dp)) {
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f))
    }
}

@Composable
private fun CollectionTab(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = if (selected) AccentRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
    ) {
        Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f), fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

private fun toggleFullscreen(context: android.content.Context) {
    val activity = context as? ComponentActivity ?: return
    val currentlyFullscreen = activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    activity.requestedOrientation = if (currentlyFullscreen) {
        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    } else {
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
    if (Build.VERSION.SDK_INT >= 30) {
        val controller = activity.window.insetsController ?: return
        if (currentlyFullscreen) controller.show(android.view.WindowInsets.Type.systemBars())
        else controller.hide(android.view.WindowInsets.Type.systemBars())
    } else {
        @Suppress("DEPRECATION")
        activity.window.decorView.systemUiVisibility = if (currentlyFullscreen) View.SYSTEM_UI_FLAG_VISIBLE else
            (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }
}