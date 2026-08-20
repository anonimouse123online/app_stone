package com.example.capstonesample.ai

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream


class ModelStatusChecker(
    private val context: Context
) {

    companion object {

        private const val TAG =
            "ModelStatusChecker"

        const val GEMMA_FILENAME =
            "gemma3-1b-it-int4.litertlm"

        private const val MIN_MODEL_SIZE =
            500_000_000L
    }


    private val modelDirectory =
        File(
            context.applicationContext.filesDir,
            "models"
        ).apply {

            if (!exists()) {
                mkdirs()
            }
        }


    // ============================================================
    // GEMMA FILE
    // ============================================================

    fun getGemmaFile(): File {

        return File(
            modelDirectory,
            GEMMA_FILENAME
        )
    }


    // ============================================================
    // GEMMA PATH
    // ============================================================

    fun getGemmaPath(): String {

        return getGemmaFile()
            .absolutePath
    }


    // ============================================================
    // CHECK MODEL
    // ============================================================

    fun isGemmaReady(): Boolean {

        val file =
            getGemmaFile()


        val ready =
            file.exists() &&
                    file.isFile &&
                    file.canRead() &&
                    file.length() >= MIN_MODEL_SIZE


        Log.d(
            TAG,
            "Gemma ready=$ready | " +
                    "exists=${file.exists()} | " +
                    "size=${file.length()} | " +
                    "path=${file.absolutePath}"
        )


        return ready
    }


    // ============================================================
    // PREPARE BUNDLED GEMMA
    // ============================================================

    fun ensureGemmaInstalled(): Boolean {

        // Already copied
        if (isGemmaReady()) {

            Log.d(
                TAG,
                "Gemma already prepared."
            )

            return true
        }


        val destination =
            getGemmaFile()


        try {

            Log.d(
                TAG,
                "Copying bundled Gemma model..."
            )


            // Delete incomplete previous copy
            if (destination.exists()) {

                destination.delete()
            }


            context.applicationContext
                .assets
                .open(GEMMA_FILENAME)
                .use { input ->

                    FileOutputStream(
                        destination
                    ).use { output ->

                        val buffer =
                            ByteArray(
                                1024 * 1024
                            )


                        while (true) {

                            val bytesRead =
                                input.read(buffer)


                            if (bytesRead == -1) {
                                break
                            }


                            output.write(
                                buffer,
                                0,
                                bytesRead
                            )
                        }


                        output.flush()
                    }
                }


            val ready =
                isGemmaReady()


            Log.d(
                TAG,
                "Gemma copy complete. Ready=$ready"
            )


            return ready


        } catch (e: Exception) {

            Log.e(
                TAG,
                "Failed to prepare bundled Gemma.",
                e
            )


            if (destination.exists()) {

                destination.delete()
            }


            return false
        }
    }


    // ============================================================
    // GENERAL AI STATUS
    // ============================================================

    fun isReady(): Boolean {

        return isGemmaReady()
    }
}