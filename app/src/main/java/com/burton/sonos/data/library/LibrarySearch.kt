package com.burton.sonos.data.library

object LibrarySearch {
    val categories = listOf(
        "Artists" to "A:ALBUMARTIST",
        "Albums" to "A:ALBUM",
        "Tracks" to "A:TRACKS",
        "Playlists" to "A:PLAYLISTS",
        "Composers" to "A:COMPOSER",
        "Genres" to "A:GENRE",
    )

    fun objectId(prefix: String, query: String): String {
        val term = query.trim()
        require(term.isNotEmpty())
        return "$prefix:$term"
    }
}
