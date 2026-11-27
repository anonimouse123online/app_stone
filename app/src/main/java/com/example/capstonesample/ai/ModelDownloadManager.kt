package com.example.capstonesample.ai

import android.content.Context
import android.util.Log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit


class ModelDownloadManager(
    context: Context
) {

    companion object {

        private const val TAG =
            "MODEL_DOWNLOAD"


        const val GEMMA_FILENAME =
            ModelStatusChecker.GEMMA_FILENAME


        const val GEMMA_DOWNLOAD_URL =
            "https://drive.usercontent.google.com/download?id=10ogxRw1GKemoSrbsBPlaPGbl9HdDKAsP&export=download&confirm=t"
    }


    private val modelDir =
        File(
            context.applicationContext.filesDir,
            "models"
        ).apply {

            if (
                !exists() &&
                !mkdirs()
            ) {

                Log.e(
                    TAG,
                    "Failed to create models directory."
                )
            }


            Log.d(
                TAG,
                "Model directory: $absolutePath"
            )
        }


    private val client =
        OkHttpClient.Builder()

            .connectTimeout(
                60,
                TimeUnit.SECONDS
            )

            .readTimeout(
                180,
                TimeUnit.SECONDS
            )

            .writeTimeout(
                180,
                TimeUnit.SECONDS
            )

            .followRedirects(
                true
            )

            .followSslRedirects(
                true
            )

            .build()


    private var currentCall: Call? =
        null


    fun downloadGemma():
            Flow<DownloadProgress> {

        return downloadFile(

            url =
                GEMMA_DOWNLOAD_URL,

            filename =
                GEMMA_FILENAME
        )
    }


    fun downloadFile(

        url: String,

        filename: String

    ): Flow<DownloadProgress> = flow {

        try {

            if (
                url.isBlank() ||
                url.contains(
                    "PUT_YOUR_GEMMA"
                )
            ) {

                emit(

                    DownloadProgress.Error(

                        filename =
                            filename,

                        message =
                            "Gemma download URL has not been configured."
                    )
                )


                return@flow
            }


            Log.d(
                TAG,
                "========================================"
            )

            Log.d(
                TAG,
                "STARTING MODEL DOWNLOAD"
            )

            Log.d(
                TAG,
                "Filename: $filename"
            )

            Log.d(
                TAG,
                "URL: $url"
            )

            Log.d(
                TAG,
                "========================================"
            )


            val finalFile =
                File(
                    modelDir,
                    filename
                )


            val tempFile =
                File(
                    modelDir,
                    "$filename.download"
                )


            if (
                finalFile.exists() &&
                finalFile.isFile &&
                finalFile.length() > 1000L
            ) {

                Log.d(
                    TAG,
                    "Model already exists."
                )

                Log.d(
                    TAG,
                    "Path: ${finalFile.absolutePath}"
                )

                Log.d(
                    TAG,
                    "Size: ${finalFile.length()} bytes"
                )


                emit(

                    DownloadProgress.FileDone(
                        filename
                    )
                )


                return@flow
            }


            var existingBytes =

                if (
                    tempFile.exists()
                ) {

                    tempFile.length()

                } else {

                    0L
                }


            Log.d(
                TAG,
                "Existing partial bytes: $existingBytes"
            )


            val requestBuilder =
                Request.Builder()

                    .url(
                        url
                    )

                    .header(
                        "User-Agent",
                        "SitePulse-Android/1.0"
                    )

                    .header(
                        "Accept",
                        "application/octet-stream,*/*"
                    )


            if (
                existingBytes > 0L
            ) {

                requestBuilder.header(
                    "Range",
                    "bytes=$existingBytes-"
                )


                Log.d(
                    TAG,
                    "Attempting resume from byte $existingBytes"
                )
            }


            val call =
                client.newCall(
                    requestBuilder.build()
                )


            currentCall =
                call


            call.execute().use { response ->


                Log.d(
                    TAG,
                    "HTTP ${response.code} ${response.message}"
                )


                if (
                    !response.isSuccessful
                ) {

                    val errorText =

                        try {

                            response.body
                                ?.string()
                                ?.take(500)

                        } catch (
                            _: Exception
                        ) {

                            null
                        }


                    Log.e(
                        TAG,
                        "HTTP error: $errorText"
                    )


                    throw IOException(
                        "HTTP ${response.code}: ${response.message}"
                    )
                }


                val body =
                    response.body
                        ?: throw IOException(
                            "Empty response body."
                        )


                val isPartialResponse =
                    response.code == 206


                /*
                 * If we requested Range but server responds
                 * with HTTP 200, restart instead of appending.
                 */
                if (
                    existingBytes > 0L &&
                    !isPartialResponse
                ) {

                    Log.w(
                        TAG,
                        "Server ignored Range. Restarting download."
                    )


                    if (
                        tempFile.exists()
                    ) {

                        tempFile.delete()
                    }


                    existingBytes =
                        0L
                }


                val responseLength =
                    body.contentLength()
                        .takeIf {

                            it >= 0L
                        }
                        ?: response
                            .header(
                                "Content-Length"
                            )
                            ?.toLongOrNull()
                        ?: 0L


                val totalBytes =

                    if (
                        isPartialResponse
                    ) {

                        existingBytes +
                                responseLength

                    } else {

                        responseLength
                    }


                Log.d(
                    TAG,
                    "Response length: $responseLength"
                )

                Log.d(
                    TAG,
                    "Expected total: $totalBytes"
                )


                val append =
                    isPartialResponse &&
                            existingBytes > 0L


                FileOutputStream(

                    tempFile,

                    append

                ).use { outputStream ->


                    body
                        .byteStream()
                        .use { inputStream ->


                            val buffer =
                                ByteArray(
                                    64 * 1024
                                )


                            var currentDownloaded =
                                existingBytes


                            var bytesRead: Int


                            var lastProgressEmit =
                                0L


                            while (

                                inputStream
                                    .read(
                                        buffer
                                    )
                                    .also {

                                        bytesRead =
                                            it

                                    } != -1
                            ) {

                                outputStream.write(

                                    buffer,

                                    0,

                                    bytesRead
                                )


                                currentDownloaded +=
                                    bytesRead


                                val now =
                                    System.currentTimeMillis()


                                if (
                                    now -
                                    lastProgressEmit >=
                                    500L
                                ) {

                                    lastProgressEmit =
                                        now


                                    val safeTotal =

                                        if (
                                            totalBytes > 0L
                                        ) {

                                            totalBytes

                                        } else {

                                            currentDownloaded
                                        }


                                    emit(

                                        DownloadProgress.Progress(

                                            filename =
                                                filename,

                                            downloaded =
                                                currentDownloaded,

                                            total =
                                                safeTotal,

                                            downloadedMB =
                                                currentDownloaded /
                                                        1_048_576L,

                                            totalMB =
                                                safeTotal /
                                                        1_048_576L
                                        )
                                    )


                                    if (
                                        totalBytes > 0L
                                    ) {

                                        val percent =
                                            (
                                                    currentDownloaded *
                                                            100L /
                                                            totalBytes
                                                    )
                                                .coerceIn(
                                                    0L,
                                                    100L
                                                )


                                        Log.d(
                                            TAG,
                                            "$filename: $percent%"
                                        )
                                    }
                                }
                            }


                            outputStream.flush()
                        }
                }
            }


            if (
                !tempFile.exists()
            ) {

                throw IOException(
                    "Downloaded model file does not exist."
                )
            }


            if (
                tempFile.length() <=
                1000L
            ) {

                throw IOException(
                    "Downloaded model file is too small."
                )
            }


            Log.d(
                TAG,
                "Temporary model size: ${tempFile.length()}"
            )


            if (
                finalFile.exists()
            ) {

                if (
                    !finalFile.delete()
                ) {

                    throw IOException(
                        "Unable to replace existing model."
                    )
                }
            }


            // TEMP -> FINAL
            val renamed =
                tempFile.renameTo(
                    finalFile
                )


            if (
                !renamed
            ) {

                Log.w(
                    TAG,
                    "renameTo failed. Falling back to copy."
                )


                tempFile
                    .inputStream()
                    .use { input ->

                        finalFile
                            .outputStream()
                            .use { output ->

                                input.copyTo(
                                    output
                                )
                            }
                    }


                if (
                    !tempFile.delete()
                ) {

                    Log.w(
                        TAG,
                        "Unable to delete temporary model file."
                    )
                }
            }


            if (
                !finalFile.exists() ||
                finalFile.length() <=
                1000L
            ) {

                throw IOException(
                    "Final model verification failed."
                )
            }


            Log.d(
                TAG,
                "========================================"
            )

            Log.d(
                TAG,
                "MODEL DOWNLOAD COMPLETE"
            )

            Log.d(
                TAG,
                "Filename: $filename"
            )

            Log.d(
                TAG,
                "Path: ${finalFile.absolutePath}"
            )

            Log.d(
                TAG,
                "Size: ${finalFile.length()} bytes"
            )

            Log.d(
                TAG,
                "Size MB: ${finalFile.length() / 1_048_576L}"
            )

            Log.d(
                TAG,
                "========================================"
            )


            emit(

                DownloadProgress.FileDone(
                    filename
                )
            )


        } catch (
            e: java.util.concurrent.CancellationException
        ) {

            Log.w(
                TAG,
                "Download coroutine cancelled."
            )


            throw e


        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "Download failed: ${e.message}",
                e
            )


            emit(

                DownloadProgress.Error(

                    filename =
                        filename,

                    message =
                        "${e.javaClass.simpleName}: " +
                                "${e.message ?: "Unknown error"}"
                )
            )

        } finally {

            currentCall =
                null
        }

    }.flowOn(
        Dispatchers.IO
    )


    fun cancelDownload() {

        Log.d(
            TAG,
            "Cancelling current download..."
        )


        currentCall
            ?.cancel()


        currentCall =
            null
    }


    fun modelExists(
        filename: String
    ): Boolean {

        val file =
            File(
                modelDir,
                filename
            )


        return file.exists() &&
                file.isFile &&
                file.length() >
                1000L
    }


    fun gemmaExists(): Boolean {

        return modelExists(
            GEMMA_FILENAME
        )
    }


    fun getModelFile(
        filename: String
    ): File {

        return File(
            modelDir,
            filename
        )
    }


    fun getGemmaFile(): File {

        return getModelFile(
            GEMMA_FILENAME
        )
    }


    fun deleteModel(
        filename: String
    ): Boolean {

        val finalFile =
            File(
                modelDir,
                filename
            )


        val tempFile =
            File(
                modelDir,
                "$filename.download"
            )


        var success =
            true


        if (
            finalFile.exists()
        ) {

            success =
                finalFile.delete() &&
                        success
        }


        if (
            tempFile.exists()
        ) {

            success =
                tempFile.delete() &&
                        success
        }


        Log.d(
            TAG,
            "Delete $filename: $success"
        )


        return success
    }


    fun deleteGemma(): Boolean {

        return deleteModel(
            GEMMA_FILENAME
        )
    }


    fun deleteAllModels() {

        cancelDownload()


        modelDir
            .listFiles()
            ?.forEach { file ->

                Log.d(
                    TAG,
                    "Deleting ${file.name}"
                )


                if (
                    !file.delete()
                ) {

                    Log.w(
                        TAG,
                        "Unable to delete ${file.name}"
                    )
                }
            }
    }
}