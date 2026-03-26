package com.danichapps.simpleagent.data.local

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.zip.ZipInputStream

class DocxTextExtractor {

    fun extract(inputStream: InputStream): String {
        val sb = StringBuilder()
        ZipInputStream(inputStream.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    extractFromXml(zip, sb)
                    break
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return sb.toString()
    }

    private fun extractFromXml(inputStream: InputStream, sb: StringBuilder) {
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        val paragraph = StringBuilder()
        var inParagraph = false
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "p" -> {
                        inParagraph = true
                        paragraph.clear()
                    }
                    "t" -> if (inParagraph) {
                        paragraph.append(runCatching { parser.nextText() }.getOrDefault(""))
                    }
                }
                XmlPullParser.END_TAG -> if (parser.name == "p") {
                    val text = paragraph.trim()
                    if (text.length >= 25) sb.appendLine(text)
                    inParagraph = false
                }
            }
            eventType = parser.next()
        }
    }
}
