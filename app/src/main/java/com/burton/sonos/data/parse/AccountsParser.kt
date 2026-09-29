package com.burton.sonos.data.parse

import com.burton.sonos.domain.LinkedAccount

object AccountsParser {
    fun parse(accountsXml: String): List<LinkedAccount> {
        if (accountsXml.isBlank()) return emptyList()
        val root = runCatching { Xml.parse(accountsXml).rootElement() }.getOrNull() ?: return emptyList()
        return root.descendants("Account").mapNotNull { account ->
            if (account.getAttribute("Deleted") == "1") return@mapNotNull null
            val serial = account.getAttribute("SerialNum")
            val type = account.getAttribute("Type")
            if (serial.isBlank() || type.isBlank()) return@mapNotNull null
            LinkedAccount(
                serialNumber = serial,
                serviceType = type,
                nickname = account.childText("NN"),
                username = account.childText("UN"),
                key = account.childText("Key"),
                oaDeviceId = account.childText("OADevID"),
            )
        }
    }
}
