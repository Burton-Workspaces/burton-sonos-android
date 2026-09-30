package com.burton.sonos.data.soap

import com.burton.sonos.data.parse.Xml
import com.burton.sonos.data.parse.child
import com.burton.sonos.data.parse.children
import com.burton.sonos.data.parse.descendants
import com.burton.sonos.data.parse.rootElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

class SoapFaultException(
    val code: String,
    message: String,
) : IOException(message)

@Singleton
class SoapClient @Inject constructor(
    private val http: OkHttpClient,
) {
    suspend fun action(
        baseUrl: String,
        controlPath: String,
        serviceType: String,
        action: String,
        args: Map<String, String> = emptyMap(),
    ): Map<String, String> = withContext(Dispatchers.IO) {
        val body = buildEnvelope(serviceType, action, args)
        val request = Request.Builder()
            .url("$baseUrl$controlPath")
            .header("Content-Type", "text/xml; charset=\"utf-8\"")
            .header("SOAPACTION", "\"$serviceType#$action\"")
            .post(body.toRequestBody(XML_MEDIA))
            .build()
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw parseFault(text) ?: IOException("SOAP $action failed: HTTP ${response.code}")
            }
            parseResponse(text, action)
        }
    }

    suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).get().build()
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("GET $url failed: HTTP ${response.code}")
            }
            text
        }
    }

    private fun buildEnvelope(
        serviceType: String,
        action: String,
        args: Map<String, String>,
    ): String {
        val argsXml = args.entries.joinToString("") { (name, value) ->
            "<$name>${Xml.escapeXml(value)}</$name>"
        }
        return """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
              <s:Body>
                <u:$action xmlns:u="$serviceType">$argsXml</u:$action>
              </s:Body>
            </s:Envelope>
        """.trimIndent()
    }

    private fun parseResponse(text: String, action: String): Map<String, String> {
        val root = Xml.parse(text).rootElement()
        val fault = root.descendants("Fault").firstOrNull()
        if (fault != null) {
            throw parseFault(text) ?: SoapFaultException("Client", "SOAP fault")
        }
        val response = root.descendants("${action}Response").firstOrNull()
            ?: root.descendants("Body").firstOrNull()?.children()?.firstOrNull()
            ?: return emptyMap()
        return response.children().associate { child ->
            val name = child.localName ?: child.nodeName.substringAfter(':')
            name to Xml.soapValue(child)
        }
    }

    private fun parseFault(text: String): SoapFaultException? {
        val root = runCatching { Xml.parse(text).rootElement() }.getOrNull() ?: return null
        val fault = root.descendants("Fault").firstOrNull() ?: return null
        val code = fault.child("errorCode")?.textContent
            ?: fault.descendants("errorCode").firstOrNull()?.textContent
            ?: fault.child("faultcode")?.textContent.orEmpty()
        val message = fault.child("faultstring")?.textContent
            ?: fault.descendants("errorDescription").firstOrNull()?.textContent
            ?: "SOAP fault $code"
        return SoapFaultException(code, message)
    }

    private companion object {
        val XML_MEDIA = "text/xml; charset=utf-8".toMediaType()
    }
}
