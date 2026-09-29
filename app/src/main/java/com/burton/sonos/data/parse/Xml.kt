package com.burton.sonos.data.parse

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

internal object Xml {
    private val factory: DocumentBuilderFactory by lazy {
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isIgnoringElementContentWhitespace = true
            isCoalescing = true
        }
    }

    fun parse(text: String): Document {
        val builder = factory.newDocumentBuilder()
        return builder.parse(ByteArrayInputStream(text.toByteArray(Charsets.UTF_8)))
    }

    fun unescapeXml(value: String): String =
        value
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")

    fun escapeXml(value: String): String =
        buildString(value.length) {
            value.forEach { ch ->
                when (ch) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&apos;")
                    else -> append(ch)
                }
            }
        }
}

internal fun Element.children(): List<Element> {
    val out = ArrayList<Element>()
    var node = firstChild
    while (node != null) {
        if (node is Element) out += node
        node = node.nextSibling
    }
    return out
}

internal fun Element.child(localName: String): Element? =
    children().firstOrNull { it.matchesName(localName) }

internal fun Element.childText(localName: String): String =
    child(localName)?.textContent?.trim().orEmpty()

internal fun Element.descendants(localName: String): List<Element> {
    val out = ArrayList<Element>()
    collect(this, localName, out)
    return out
}

internal fun Element.matchesName(localName: String): Boolean {
    val local = this.localName ?: nodeName.substringAfter(':')
    return local.equals(localName, ignoreCase = true)
}

internal fun Document.rootElement(): Element = documentElement

private fun collect(node: Node, localName: String, out: MutableList<Element>) {
    if (node is Element && node.matchesName(localName)) {
        out += node
    }
    var child = node.firstChild
    while (child != null) {
        collect(child, localName, out)
        child = child.nextSibling
    }
}
