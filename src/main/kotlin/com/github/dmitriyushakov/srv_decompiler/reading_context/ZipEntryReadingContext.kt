package com.github.dmitriyushakov.srv_decompiler.reading_context

import java.io.InputStream
import java.util.zip.ZipInputStream

class ZipEntryReadingContext(val entryName: String, val archiveStreamContext: ReadingContext): ReadingContext {
    override fun use(actions: (InputStream) -> Unit) {
        archiveStreamContext.use { archiveStream ->
            ZipInputStream(archiveStream).use { zipInputStream ->
                var entry = zipInputStream.getNextEntry()

                while (entry != null) {
                    if (entry.name == entryName) {
                        actions(zipInputStream)
                        zipInputStream.closeEntry()
                        break
                    } else {
                        zipInputStream.closeEntry()
                    }

                    entry = zipInputStream.getNextEntry()
                }
            }
        }
    }
}