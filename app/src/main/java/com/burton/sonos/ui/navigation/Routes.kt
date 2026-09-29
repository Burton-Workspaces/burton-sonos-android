package com.burton.sonos.ui.navigation

object Routes {
    const val ROOMS = "rooms"
    const val ROOM = "room/{groupId}"
    const val SOURCES = "sources"
    const val BROWSE = "browse/{objectId}?title={title}"
    const val SPOTIFY = "spotify"
    const val SPOTIFY_BROWSE = "spotifyBrowse/{itemId}?title={title}"

    fun room(groupId: String) = "room/${enc(groupId)}"
    fun browse(objectId: String, title: String) =
        "browse/${enc(objectId)}?title=${enc(title)}"
    fun spotifyBrowse(itemId: String, title: String) =
        "spotifyBrowse/${enc(itemId)}?title=${enc(title)}"

    private fun enc(value: String) = android.net.Uri.encode(value)
}
