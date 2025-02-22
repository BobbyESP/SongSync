package pl.lambada.songsync.data.remote.lyrics_providers.others

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.lambada.songsync.data.remote.PaxMusicHelper
import pl.lambada.songsync.domain.model.SongInfo
import pl.lambada.songsync.domain.model.lyrics_providers.others.AppleSearchResponse
import pl.lambada.songsync.util.EmptyQueryException
import pl.lambada.songsync.util.networking.Ktor.client
import pl.lambada.songsync.util.networking.Ktor.json
import java.net.URLEncoder

class AppleAPI {
    private val baseURL = "https://paxsenix.alwaysdata.net/"

    suspend fun getSongInfo(query: SongInfo, offset: Int = 0): SongInfo? {
        val search = withContext(Dispatchers.IO) {
            URLEncoder.encode(
                "${query.songName} ${query.artistName}",
                Charsets.UTF_8.toString()
            )
        }

        if (search == " ")
            throw EmptyQueryException()

        val response = client.get(
            baseURL + "searchAppleMusic.php?q=$search"
        )
        val responseBody = response.bodyAsText(Charsets.UTF_8)

        if (response.status.value !in 200..299)
            return null

        val json = try {
            json.decodeFromString<List<AppleSearchResponse>>(responseBody)
        } catch (e: Exception) {
            return null
        }

        val result = try {
            json[offset]
        } catch (e: IndexOutOfBoundsException) {
            return null
        }

        return SongInfo(
            songName = result.songName,
            artistName = result.artistName,
            songLink = result.url,
            albumCoverLink = result.artwork.replace("{w}", "100").replace("{h}", "100")
                .replace("{f}", "png"),
            appleID = result.id
        )
    }

    suspend fun getSyncedLyrics(id: Long, multiPersonWordByWord: Boolean): String? {
        val response = client.get(
            baseURL + "getAppleMusicLyrics.php?id=$id"
        )
        val responseBody = response.bodyAsText(Charsets.UTF_8)

        if (response.status.value !in 200..299 || responseBody.isEmpty())
            return null

        return PaxMusicHelper().formatWordByWordLyrics(responseBody, multiPersonWordByWord)
    }
}