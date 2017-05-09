package com.ivieleague.wardtextsender

import android.app.Activity
import android.content.IntentSender
import android.os.Bundle
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.drive.*
import com.lightningkite.kotlin.anko.viewcontrollers.implementations.VCActivity
import java.io.InputStream

fun VCActivity.getGoogleDriveFileInputStream(
        types: List<String> = listOf("text/csv"),
        onCancel: () -> Unit,
        onError: (Throwable) -> Unit = { onCancel() },
        onStreamObtained: (InputStream) -> Unit
) = getGoogleDriveFile(types, onCancel, onError, onFileObtained = { client, file ->
    file.open(client, DriveFile.MODE_READ_ONLY, null).setResultCallback {
        try {
            println("Reading")
            val contents = it.driveContents
            onStreamObtained(contents.inputStream)
            client.disconnect()
        } catch(e: Throwable) {
            onError.invoke(e)
            client.disconnect()
        }
    }
})

fun VCActivity.getGoogleDriveFileOverwrite(
        data: ByteArray,
        types: List<String> = listOf("text/csv"),
        onCancel: () -> Unit,
        onError: (Throwable) -> Unit = { onCancel() },
        onDone: () -> Unit
) = getGoogleDriveFile(types, onCancel, onError, onFileObtained = { client, file ->
    file.open(client, DriveFile.MODE_WRITE_ONLY, null).setResultCallback {
        try {
            val outputStream = it.driveContents.outputStream
            outputStream.write(data)
            outputStream.close()
            onDone()
            client.disconnect()
        } catch(e: Throwable) {
            onError.invoke(e)
            client.disconnect()
        }
    }
})

fun VCActivity.newGoogleDriveFile(
        dialogTitle: String,
        data: ByteArray,
        type: String = "text/csv",
        onCancel: () -> Unit,
        onError: (Throwable) -> Unit = { onCancel() },
        onDone: () -> Unit
) = newGoogleDriveFile(dialogTitle, type, onCancel, onError, onFileObtained = { client, file ->
    file.open(client, DriveFile.MODE_WRITE_ONLY, null).setResultCallback {
        try {
            val outputStream = it.driveContents.outputStream
            outputStream.write(data)
            outputStream.close()
            onDone()
            client.disconnect()
        } catch(e: Throwable) {
            onError.invoke(e)
            client.disconnect()
        }
    }
})

fun VCActivity.getGoogleDriveFile(
        types: List<String> = listOf("text/csv"),
        onCancel: () -> Unit,
        onError: (Throwable) -> Unit = { onCancel() },
        onFileObtained: (GoogleApiClient, DriveFile) -> Unit
) = doWithClient(onCancel, onError, onClient = { client ->

    val sender = Drive.DriveApi.newOpenFileActivityBuilder()
            .setMimeType(types.toTypedArray())
            .build(client)
    this@getGoogleDriveFile.startIntentSenderForResult(sender, this@getGoogleDriveFile.prepareOnResult { code, data ->
        if (code == Activity.RESULT_OK) {
            try {
                val fileId: DriveId = data!!.getParcelableExtra(OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID)
                val file = fileId.asDriveFile()
                println("File obtained")
                onFileObtained(client, file)
            } catch (e: Throwable) {
                onError(e)
                client.disconnect()
            }
        } else {
            println("Canceled $code")
            onCancel()
            client.disconnect()
        }
    }, null, 0, 0, 0)
})

fun VCActivity.newGoogleDriveFile(
        dialogTitle: String,
        type: String = "text/csv",
        onCancel: () -> Unit,
        onError: (Throwable) -> Unit = { onCancel() },
        onFileObtained: (GoogleApiClient, DriveFile) -> Unit
) = doWithClient(onCancel, onError, onClient = { client ->

    val sender = Drive.DriveApi.newCreateFileActivityBuilder()
            .setActivityTitle(dialogTitle)
            .setInitialMetadata(MetadataChangeSet.Builder().setMimeType(type).build())
            .build(client)
    this@newGoogleDriveFile.startIntentSenderForResult(sender, this@newGoogleDriveFile.prepareOnResult { code, data ->
        if (code == Activity.RESULT_OK) {
            try {
                val fileId: DriveId = data!!.getParcelableExtra(CreateFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID)
                val file = fileId.asDriveFile()
                println("File obtained")
                onFileObtained(client, file)
            } catch (e: Throwable) {
                onError(e)
                client.disconnect()
            }
        } else {
            println("Canceled $code")
            onCancel()
            client.disconnect()
        }
    }, null, 0, 0, 0)
})

fun VCActivity.doWithClient(
        onCancel: () -> Unit,
        onError: (Throwable) -> Unit = { onCancel() },
        onClient: (GoogleApiClient) -> Unit
) {
    var client: GoogleApiClient? = null
    client = GoogleApiClient.Builder(this)
            .addApi(Drive.API)
            .addScope(Drive.SCOPE_FILE)
            .addConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
                override fun onConnected(p0: Bundle?) {
                    println("Connected")
                    onClient.invoke(client!!)
                }

                override fun onConnectionSuspended(p0: Int) {
                }
            })
            .addOnConnectionFailedListener { connectionResult ->
                println("Failed + $connectionResult")
                if (connectionResult.hasResolution()) {
                    try {
                        connectionResult.startResolutionForResult(this@doWithClient, this@doWithClient.prepareOnResult { code, intent ->
                            println("Resolution callback $code")
                            if (code == Activity.RESULT_OK) {
                                println("Attempting resolution")
                                client?.connect()
                            } else {
                                onCancel()
                                client?.disconnect()
                            }
                        })
                    } catch (e: IntentSender.SendIntentException) {
                        println(e.printStackTrace())
                        onCancel()
                        client?.disconnect()
                    }

                } else {
                    onCancel()
                    client?.disconnect()
                }
            }
            .build()
    client?.connect()
    println("Connecting")
}