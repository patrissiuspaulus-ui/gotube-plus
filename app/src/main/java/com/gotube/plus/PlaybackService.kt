package com.gotube.plus

import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private lateinit var libraryStore: LibraryStore
    private val handler = Handler(Looper.getMainLooper())
    private val savePositionTask = object : Runnable {
        override fun run() {
            savePlaybackState()
            handler.postDelayed(this, SAVE_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        libraryStore = LibraryStore(this)
        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            setHandleAudioBecomingNoisy(true)
        }

        val mediaItems = VideoCatalog.items.map(VideoItem::toMediaItem)
        val savedPlayback = libraryStore.savedPosition()
        val savedIndex = savedPlayback?.first?.let { id ->
            VideoCatalog.items.indexOfFirst { it.id == id }
        } ?: -1
        if (savedIndex >= 0) {
            player.setMediaItems(mediaItems, savedIndex, savedPlayback!!.second)
        }

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                savePlaybackState()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                savePlaybackState()
            }
        })

        if (savedIndex >= 0) {
            player.playWhenReady = libraryStore.wasPlaying()
            player.prepare()
        }
        mediaSession = MediaSession.Builder(this, player).build()
        handler.postDelayed(savePositionTask, SAVE_INTERVAL_MS)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        if (!player.isPlaying && !player.playWhenReady) stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(savePositionTask)
        savePlaybackState()
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    private fun savePlaybackState() {
        if (!::player.isInitialized || !::libraryStore.isInitialized) return
        val mediaId = player.currentMediaItem?.mediaId ?: return
        libraryStore.savePlayback(
            id = mediaId,
            position = player.currentPosition,
            playing = player.playWhenReady
        )
    }

    private companion object {
        const val SAVE_INTERVAL_MS = 10_000L
    }
}