package com.burton.sonos.data.parse

import com.burton.sonos.domain.MusicServiceDescriptor

object MusicServicesParser {
    fun parse(descriptorListXml: String): List<MusicServiceDescriptor> {
        if (descriptorListXml.isBlank()) return emptyList()
        val xml = Xml.unescapeXml(descriptorListXml).ifBlank { descriptorListXml }
        val wrapped = if (xml.contains("<Services")) xml else "<Services>$xml</Services>"
        val root = runCatching { Xml.parse(wrapped).rootElement() }.getOrNull() ?: return emptyList()
        return root.descendants("Service").mapNotNull { service ->
            val id = service.getAttribute("Id").ifBlank { return@mapNotNull null }
            val name = service.getAttribute("Name").ifBlank { return@mapNotNull null }
            val serviceType = (id.toIntOrNull()?.let { it * 256 + 7 } ?: 0).toString()
            val policy = service.child("Policy")
            MusicServiceDescriptor(
                id = id,
                name = name,
                serviceType = service.getAttribute("Type").ifBlank { serviceType },
                auth = policy?.getAttribute("Auth").orEmpty(),
                uri = service.getAttribute("Uri"),
                secureUri = service.getAttribute("SecureUri").ifBlank { service.getAttribute("Uri") },
                capabilities = service.getAttribute("Capabilities"),
            )
        }
    }
}
