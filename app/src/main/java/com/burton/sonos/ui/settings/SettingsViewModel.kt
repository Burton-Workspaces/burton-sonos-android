package com.burton.sonos.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.sonos.data.repository.LocalPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: LocalPrefs,
) : ViewModel() {
    val grayscaleAlbumArt = prefs.grayscaleAlbumArt.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        false,
    )

    fun setGrayscaleAlbumArt(enabled: Boolean) {
        viewModelScope.launch { prefs.setGrayscaleAlbumArt(enabled) }
    }
}
