package com.burton.sonos.data.parse

object DeviceDescriptionParser {
    data class DeviceInfo(
        val uuid: String,
        val roomName: String,
        val modelName: String,
        val displayName: String,
        val householdId: String,
    )

    fun parse(xml: String): DeviceInfo? {
        val root = runCatching { Xml.parse(xml).rootElement() }.getOrNull() ?: return null
        val udn = root.descendants("UDN").firstOrNull()?.textContent.orEmpty()
        val uuid = udn.removePrefix("uuid:")
        if (uuid.isBlank()) return null
        return DeviceInfo(
            uuid = uuid,
            roomName = root.descendants("roomName").firstOrNull()?.textContent?.trim().orEmpty(),
            modelName = root.descendants("modelName").firstOrNull()?.textContent?.trim().orEmpty(),
            displayName = root.descendants("displayName").firstOrNull()?.textContent?.trim().orEmpty(),
            householdId = root.descendants("householdID").firstOrNull()?.textContent?.trim()
                ?: root.descendants("householdId").firstOrNull()?.textContent?.trim().orEmpty(),
        )
    }
}
