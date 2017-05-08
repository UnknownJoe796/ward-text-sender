package com.ivieleague.wardtextsender

import android.app.Activity
import android.content.IntentSender
import android.os.Bundle
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.drive.Drive
import com.google.android.gms.drive.DriveFile
import com.google.android.gms.drive.DriveId
import com.google.android.gms.drive.OpenFileActivityBuilder
import com.lightningkite.kotlin.anko.viewcontrollers.implementations.VCActivity
import java.io.InputStream

fun VCActivity.getGoogleDriveFileInputStream(
        types: List<String> = listOf("text/csv"),
        onCancel: () -> Unit,
        onError: (Throwable) -> Unit = { onCancel() },
        onDone: (InputStream) -> Unit
) {
    var client: GoogleApiClient? = null
    client = GoogleApiClient.Builder(this)
            .addApi(Drive.API)
            .addScope(Drive.SCOPE_FILE)
            .addConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
                override fun onConnected(p0: Bundle?) {
                    println("Connected")
                    val sender = Drive.DriveApi.newOpenFileActivityBuilder()
                            .setMimeType(types.toTypedArray())
                            .build(client)
                    this@getGoogleDriveFileInputStream.startIntentSenderForResult(sender, this@getGoogleDriveFileInputStream.prepareOnResult { code, data ->
                        if (code == Activity.RESULT_OK) {
                            try {
                                val fileId: DriveId = data!!.getParcelableExtra(OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID)
                                val file = fileId.asDriveFile()
                                file.open(client, DriveFile.MODE_READ_ONLY, null).setResultCallback {
                                    try {
                                        val contents = it.driveContents
                                        onDone(contents.inputStream)
                                        client?.disconnect()
                                    } catch (e: Throwable) {
                                        onError(e)
                                        client?.disconnect()
                                    }
                                }
                            } catch (e: Throwable) {
                                onError(e)
                                client?.disconnect()
                            }
                        } else {
                            println("Canceled $code")
                            onCancel()
                            client?.disconnect()
                        }
                    }, null, 0, 0, 0)
                }

                override fun onConnectionSuspended(p0: Int) {
                }
            })
            .addOnConnectionFailedListener { connectionResult ->
                println("Failed + $connectionResult")
                if (connectionResult.hasResolution()) {
                    try {
                        connectionResult.startResolutionForResult(this@getGoogleDriveFileInputStream, this@getGoogleDriveFileInputStream.prepareOnResult { code, intent ->
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