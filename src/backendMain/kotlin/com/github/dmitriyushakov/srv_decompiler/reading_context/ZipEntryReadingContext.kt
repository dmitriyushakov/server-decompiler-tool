package com.github.dmitriyushakov.srv_decompiler.reading_context

import java.io.InputStream
import java.util.zip.ZipInputStream

class ZipEntryReadingContext(val entryName: String, val archiveStreamContext: ReadingContext): ReadingContext {
    override fun <R> use(actions: (InputStream) -> R): R {
        return archiveStreamContext.use { archiveStream ->
            ZipInputStream(archiveStream).use { zipInputStream ->
                var result: R? = null
                var entry = zipInputStream.getNextEntry()

                while (entry != null) {
                    if (entry.name == entryName) {
                        result = actions(zipInputStream)
                        zipInputStream.closeEntry()
                        break
                    } else {
                        zipInputStream.closeEntry()
                    }

                    entry = zipInputStream.getNextEntry()
                }

                result ?: error("Unable to find zip entry with \"$entryName\" name.")
            }
        }
    }

    override val readingDataPath: String
        get() = "${archiveStreamContext.readingDataPath}/$entryName"
}