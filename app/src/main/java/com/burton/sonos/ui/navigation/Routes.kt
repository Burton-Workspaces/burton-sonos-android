package com.burton.sonos.ui.navigation

object Routes {
    const val ROOMS = "rooms"
    const val ROOM = "room/{groupId}"
    const val GROUPS = "groups"
    const val SOURCES = "sources"
    const val SEARCH = "search"
    const val BROWSE = "browse/{objectId}?title={title}"
    const val ALARMS = "alarms"
    const val ALARM_EDIT = "alarm/{alarmId}"

    fun room(groupId: String) = "room/${enc(groupId)}"
    fun browse(objectId: String, title: String) =
        "browse/${enc(objectId)}?title=${enc(title)}"
    fun alarmEdit(alarmId: String) = "alarm/${enc(alarmId)}"
    fun alarmNew() = "alarm/new"

    private fun enc(value: String) = android.net.Uri.encode(value)
}
