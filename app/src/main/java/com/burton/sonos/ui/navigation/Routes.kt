package com.burton.sonos.ui.navigation

object Routes {
    const val ROOMS = "rooms"
    const val ROOM = "room/{groupId}"
    const val SOURCES = "sources"
    const val SEARCH = "search"
    const val BROWSE = "browse/{objectId}?title={title}"

    fun room(groupId: String) = "room/${enc(groupId)}"
    fun browse(objectId: String, title: String) =
        "browse/${enc(objectId)}?title=${enc(title)}"

    private fun enc(value: String) = android.net.Uri.encode(value)
}
