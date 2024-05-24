package pl.lambada.songsync.data

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.lambada.songsync.MainActivity
import pl.lambada.songsync.MainActivity.Companion.mediaStore
import pl.lambada.songsync.data.remote.github.GithubAPI
import pl.lambada.songsync.data.remote.lyrics_providers.others.LRCLibAPI
import pl.lambada.songsync.data.remote.lyrics_providers.others.NeteaseAPI
import pl.lambada.songsync.data.remote.lyrics_providers.spotify.SpotifyAPI
import pl.lambada.songsync.data.remote.lyrics_providers.spotify.SpotifyLyricsAPI
import pl.lambada.songsync.domain.model.Release
import pl.lambada.songsync.domain.model.Song
import pl.lambada.songsync.domain.model.SongInfo
import pl.lambada.songsync.ui.screens.Providers
import pl.lambada.songsync.util.ext.getVersion
import pl.lambada.songsync.util.ext.toLrcFile
import java.io.FileNotFoundException
import java.net.UnknownHostException

/**
 * ViewModel class for the main functionality of the app.
 */
class MainViewModel : ViewModel() {

    private val mutableViewStateFlow = MutableStateFlow(DownloaderViewState())
    val viewStateFlow = mutableViewStateFlow.asStateFlow()
    data class DownloaderViewState(
        var songsList: List<Song>? = null,
    )

    var nextSong: Song? = null // for fullscreen downloader dialog

    // Filter settings
    var blacklistedFolders = mutableListOf<String>()
    var hideLyrics = false
    private var hideFolders = blacklistedFolders.isNotEmpty()
    var cachedFilteredSongs: List<Song>? = null

    // Spotify API token
    private val spotifyAPI = SpotifyAPI()

    // other settings
    var pureBlack: MutableState<Boolean> = mutableStateOf(false)
    var sdCardPath = ""

    // selected provider
    var provider = Providers.SPOTIFY

    // LRCLib Track ID
    var lrcLibID = 0

    // Netease Track ID
    var neteaseID = 0
    // TODO: Use values from SongInfo object returned by search instead of storing them here

    var downloadJob: Job? = null

    /**
     * Refreshes the access token by sending a request to the Spotify API.
     */
    suspend fun refreshToken() {
        spotifyAPI.refreshToken()
    }

    fun loadSongs(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val songs = mediaStore.getAllSongs(context = context)
            mutableViewStateFlow.update {
                it.copy(songsList = songs)
            }
        }
    }

    /**
     * Gets song information from the Spotify API.
     * @param query The SongInfo object with songName and artistName fields filled.
     * @param offset (optional) The offset used for trying to find a better match or searching again.
     * @return The SongInfo object containing the song information.
     */
    @Throws(UnknownHostException::class, FileNotFoundException::class, NoTrackFoundException::class)
    suspend fun getSongInfo(query: SongInfo, offset: Int? = 0): SongInfo {
        return try {
            when (this.provider) {
                Providers.SPOTIFY -> spotifyAPI.getSongInfo(query, offset)
                Providers.LRCLIB -> LRCLibAPI().getSongInfo(query).also {
                    this.lrcLibID = it?.lrcLibID ?: 0
                } ?: throw NoTrackFoundException()

                Providers.NETEASE -> NeteaseAPI().getSongInfo(query, offset).also {
                    this.neteaseID = it?.neteaseID ?: 0
                } ?: throw NoTrackFoundException()
            }
        } catch (e: Exception) {
            throw NoTrackFoundException()
        }
    }

    /**
     * Gets synced lyrics using the song link and returns them as a string formatted as an LRC file.
     * @param songLink The link to the song.
     * @return The synced lyrics as a string.
     */
    suspend fun getSyncedLyrics(songLink: String): String? {
        return try {
            when (this.provider) {
                Providers.SPOTIFY -> SpotifyLyricsAPI().getSyncedLyrics(songLink)
                Providers.LRCLIB -> LRCLibAPI().getSyncedLyrics(this.lrcLibID)
                Providers.NETEASE -> NeteaseAPI().getSyncedLyrics(this.neteaseID)
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun downloadBatchSongs(callback: (BatchDownloadInfo, BatchDownloadProgress) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {

        }
    }

    /**
     * Gets latest GitHub release information.
     * @return The latest release version.
     */
    suspend fun getLatestRelease(): Release {
        return GithubAPI.getLatestRelease()
    }

    /**
     * Checks if the latest release is newer than the current version.
     */
    suspend fun isNewerRelease(context: Context): Boolean {
        val currentVersion = context.getVersion().replace(".", "").toInt()
        val latestVersion = getLatestRelease().tagName.replace(".", "").replace("v", "").toInt()

        return latestVersion > currentVersion
    }

    /**
     * Filter songs based on user's preferences.
     * @return A list of songs depending on the user's preferences. If no preferences are set, null is returned, so app will use all songs.
     */
    fun filterSongs(): List<Song>? {
        hideFolders = blacklistedFolders.isNotEmpty()
        return when {
            hideLyrics && hideFolders -> {
                mediaStore.cachedSongs!!
                    .filter {
                        it.filePath.toLrcFile()?.exists() != true && !blacklistedFolders.contains(
                            it.filePath!!.substring(
                                0, it.filePath.lastIndexOf("/")
                            )
                        )
                    }
                    .also { cachedFilteredSongs = it }
            }

            hideLyrics -> {
                mediaStore.cachedSongs!!
                    .filter { it.filePath.toLrcFile()?.exists() != true }
                    .also { cachedFilteredSongs = it }
            }

            hideFolders -> {
                mediaStore.cachedSongs!!.filter {
                    !blacklistedFolders.contains(
                        it.filePath!!.substring(
                            0,
                            it.filePath.lastIndexOf("/")
                        )
                    )
                }
            }

            else -> {
                null.also {
                    cachedFilteredSongs = null
                }
            }
        }
    }
}

data class BatchDownloadInfo(
    val songName: String,
    val totalSongs: Int,
    val alreadyDownloadedSongs: Int,
)

data class BatchDownloadProgress(
    val successful: Int,
    val failed: Int,
    val noLyrics: Int,
)

class NoTrackFoundException : Exception()

class EmptyQueryException : Exception()