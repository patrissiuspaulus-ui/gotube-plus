package com.gotube.plus

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

data class VideoItem(
    val id: String,
    val title: String,
    val creator: String,
    val description: String,
    val streamUrl: String,
    val thumbnailUrl: String,
    val durationLabel: String
) {
    fun toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setUri(Uri.parse(streamUrl))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(creator)
                .setArtworkUri(Uri.parse(thumbnailUrl))
                .build()
        )
        .build()
}

object VideoCatalog {
    val items = listOf(
        VideoItem(
            id = "big-buck-bunny",
            title = "Big Buck Bunny",
            creator = "Blender Foundation",
            description = "Film animasi pendek yang dirilis dengan lisensi Creative Commons.",
            streamUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            thumbnailUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg",
            durationLabel = "9:56"
        ),
        VideoItem(
            id = "elephants-dream",
            title = "Elephants Dream",
            creator = "Blender Foundation",
            description = "Film terbuka dari Blender Foundation untuk demonstrasi pemutaran.",
            streamUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            thumbnailUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/images/ElephantsDream.jpg",
            durationLabel = "10:53"
        ),
        VideoItem(
            id = "for-bigger-joyrides",
            title = "For Bigger Joyrides",
            creator = "Google Media Samples",
            description = "Video sampel langsung untuk menguji pemutar GoTube Plus.",
            streamUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
            thumbnailUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerJoyrides.jpg",
            durationLabel = "0:15"
        ),
        VideoItem(
            id = "sintel",
            title = "Sintel",
            creator = "Blender Foundation",
            description = "Film pendek Blender Foundation yang tersedia untuk ditonton daring.",
            streamUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            thumbnailUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/images/Sintel.jpg",
            durationLabel = "14:48"
        )
    )

    fun find(id: String?): VideoItem? = items.firstOrNull { it.id == id }
}