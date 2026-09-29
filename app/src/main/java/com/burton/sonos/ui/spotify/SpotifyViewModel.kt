package com.burton.sonos.ui.spotify

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.sonos.data.repository.SonosRepository
import com.burton.sonos.domain.BrowseItem
import com.burton.sonos.domain.SpotifyLinkSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SpotifyUiState(
    val connected: Boolean = false,
    val nickname: String = "",
    val linking: Boolean = false,
    val message: String? = null,
    val session: SpotifyLinkSession? = null,
    val items: List<BrowseItem> = emptyList(),
    val loadingBrowse: Boolean = false,
)

@HiltViewModel
class SpotifyViewModel @Inject constructor(
    private val repository: SonosRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow(SpotifyUiState())
    val ui = _ui.asStateFlow()
    val household = repository.state
    private var pollJob: Job? = null

    init {
        viewModelScope.launch {
            repository.state.collect { snapshot ->
                _ui.update {
                    it.copy(
                        connected = snapshot.spotifyAccount != null,
                        nickname = snapshot.spotifyAccount?.nickname
                            ?: snapshot.spotifyAccount?.username.orEmpty(),
                    )
                }
            }
        }
        loadRoot()
    }

    fun loadRoot() {
        viewModelScope.launch {
            _ui.update { it.copy(loadingBrowse = true) }
            val items = runCatching { repository.browseSpotify("root") }.getOrDefault(emptyList())
            _ui.update { it.copy(items = items, loadingBrowse = false) }
        }
    }

    fun play(item: BrowseItem) {
        viewModelScope.launch { repository.playItem(item) }
    }

    fun startLink(context: Context) {
        viewModelScope.launch {
            _ui.update { it.copy(linking = true, message = "Opening Spotify authorization…") }
            val session = runCatching { repository.beginSpotifyLink() }.getOrElse { error ->
                _ui.update {
                    it.copy(
                        linking = false,
                        message = error.message ?: "Couldn't start Spotify linking.",
                    )
                }
                return@launch
            }
            _ui.update { it.copy(session = session, message = "Sign in, then return here. We'll finish linking automatically.") }
            openUrl(context, session.regUrl)
            pollJob?.cancel()
            pollJob = viewModelScope.launch {
                repeat(90) {
                    if (!isActive) return@launch
                    delay(2_000)
                    val done = runCatching { repository.completeSpotifyLink(session) }.getOrDefault(false)
                    if (done) {
                        _ui.update {
                            it.copy(
                                linking = false,
                                connected = true,
                                message = "Spotify is on this household.",
                            )
                        }
                        loadRoot()
                        return@launch
                    }
                }
                _ui.update { it.copy(linking = false, message = "Linking timed out. Start again after signing in.") }
            }
        }
    }

    private fun openUrl(context: Context, url: String) {
        val parsed = url.toUri()
        runCatching {
            CustomTabsIntent.Builder().build().launchUrl(context, parsed)
        }.onFailure {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, parsed)
            context.startActivity(intent)
        }
    }
}
