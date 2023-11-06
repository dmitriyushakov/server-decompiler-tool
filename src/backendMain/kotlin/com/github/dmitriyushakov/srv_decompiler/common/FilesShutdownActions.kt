package com.github.dmitriyushakov.srv_decompiler.common

import org.slf4j.LoggerFactory
import java.io.File

object FilesShutdownActions {
    private val logger = LoggerFactory.getLogger(FilesShutdownActions::class.java)
    private var scheduled: Boolean = false

    private val itemsToClose: MutableList<AutoCloseable> = mutableListOf()
    private val filesToDelete: MutableList<File> = mutableListOf()

    private object ShutdownHook: Runnable {
        override fun run() {
            for (item in itemsToClose) {
                try {
                    item.close()
                } catch (ex: Exception) {
                    logger.error("Exception caused during trying to close file in shutdown hook!", ex)
                }
            }

            for (file in filesToDelete) file.delete()
        }
    }

    private fun schedule() {
        if (!scheduled) {
            val th = Thread(ShutdownHook)
            Runtime.getRuntime().addShutdownHook(th)
            scheduled = true
        }
    }

    fun toClose(item: AutoCloseable) {
        synchronized(this) {
            schedule()
            itemsToClose.add(item)
        }
    }

    fun toDelete(file: File) {
        synchronized(this) {
            schedule()
            filesToDelete.add(file)
        }
    }
}