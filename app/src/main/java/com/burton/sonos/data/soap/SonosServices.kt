package com.burton.sonos.data.soap

object SonosServices {
    const val AV_TRANSPORT = "urn:schemas-upnp-org:service:AVTransport:1"
    const val RENDERING_CONTROL = "urn:schemas-upnp-org:service:RenderingControl:1"
    const val GROUP_RENDERING = "urn:schemas-upnp-org:service:GroupRenderingControl:1"
    const val ZONE_GROUP_TOPOLOGY = "urn:schemas-upnp-org:service:ZoneGroupTopology:1"
    const val DEVICE_PROPERTIES = "urn:schemas-upnp-org:service:DeviceProperties:1"
    const val CONTENT_DIRECTORY = "urn:schemas-upnp-org:service:ContentDirectory:1"
    const val MUSIC_SERVICES = "urn:schemas-upnp-org:service:MusicServices:1"
    const val SYSTEM_PROPERTIES = "urn:schemas-upnp-org:service:SystemProperties:1"

    const val AV_TRANSPORT_PATH = "/MediaRenderer/AVTransport/Control"
    const val RENDERING_CONTROL_PATH = "/MediaRenderer/RenderingControl/Control"
    const val GROUP_RENDERING_PATH = "/MediaRenderer/GroupRenderingControl/Control"
    const val ZONE_GROUP_TOPOLOGY_PATH = "/ZoneGroupTopology/Control"
    const val DEVICE_PROPERTIES_PATH = "/DeviceProperties/Control"
    const val CONTENT_DIRECTORY_PATH = "/MediaServer/ContentDirectory/Control"
    const val MUSIC_SERVICES_PATH = "/MusicServices/Control"
    const val SYSTEM_PROPERTIES_PATH = "/SystemProperties/Control"
}
