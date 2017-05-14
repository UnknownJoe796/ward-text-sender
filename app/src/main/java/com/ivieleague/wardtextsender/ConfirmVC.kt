package com.ivieleague.wardtextsender

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.support.v4.app.NotificationCompat
import android.telephony.SmsManager
import android.view.View
import com.lightningkite.kotlin.anko.viewcontrollers.AnkoViewController
import com.lightningkite.kotlin.anko.viewcontrollers.implementations.VCActivity
import org.jetbrains.anko.*

/**
 * Created by josep on 5/14/2017.
 */
class ConfirmVC(val contacts: List<Contact>, val message: String) : AnkoViewController() {

    override fun getTitle(resources: Resources): String = resources.getString(R.string.confirm)

    override fun createView(ui: AnkoContext<VCActivity>): View = ui.scrollView {
        verticalLayout {
            padding = dip(8)

            textView {
                styleDefault()
                textResource = R.string.confirm_message
            }.lparams(matchParent, wrapContent) { margin = dip(8) }

            textView {
                styleDefault()
                textResource = R.string.recipients
            }.lparams(matchParent, wrapContent) { margin = dip(8) }

            textView {
                styleDefault()
                text = contacts.joinToString("\n") { it.name + " - " + it.phoneNumber }
            }.lparams(matchParent, wrapContent) { margin = dip(8); leftMargin = dip(16) }

            textView {
                styleDefault()
                textResource = R.string.message_contents
            }.lparams(matchParent, wrapContent) { margin = dip(8) }

            textView {
                styleDefault()
                text = message
            }.lparams(matchParent, wrapContent) { margin = dip(8); leftMargin = dip(16) }

            button {
                textResource = R.string.send

                onClick {

                    val notificationBuilder = NotificationCompat.Builder(context)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentTitle("Sending texts...")
                            .setContentText("0 / ${contacts.size}")
                            .setProgress(contacts.size, 0, false)
                    ui.owner.sendSmsList(
                            numbers = contacts.map { it.phoneNumber },
                            message = message,
                            onProgress = { index, errors ->
                                notificationBuilder.setContentText("$index / ${contacts.size}")
                                notificationBuilder.setProgress(contacts.size, index, false)
                                context.notificationManager.notify(0, notificationBuilder.build())
                            },
                            onComplete = { errors ->
                                notificationBuilder.setContentText("Complete")
                                notificationBuilder.setProgress(contacts.size, contacts.size, false)
                                context.notificationManager.notify(0, notificationBuilder.build())
                            }
                    )

                }
            }
        }
    }

    fun VCActivity.sendSmsList(numbers: List<String>, message: String, onProgress: (count: Int, errorCount: Int) -> Unit, onComplete: (errorCount: Int) -> Unit) {
        var index = 0
        var errorCount = 0
        fun sendNext() {
            if (index >= numbers.size) {
                onComplete.invoke(errorCount)
            } else {
                onProgress.invoke(index, errorCount)
                sendSms(numbers[index], message, onError = { errorCount++ }, onComplete = {
                    sendNext()
                })
            }
            index++
        }
        if (numbers.isNotEmpty()) {
            sendNext()
        }
    }

    fun VCActivity.sendSms(number: String, message: String, onError: (code: Int) -> Unit, onComplete: () -> Unit) {
        requestPermission(Manifest.permission.SEND_SMS) {
            if (it) {
                println("Sending...")
                try {
                    registerReceiver(object : BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent?) {
                            println("RESULT! $resultCode")
                            when (resultCode) {
                                Activity.RESULT_OK -> {
                                    onComplete.invoke()
                                }
                                else -> {
                                    onError.invoke(resultCode)
                                }
                            }
                            unregisterReceiver(this)
                        }
                    }, IntentFilter(SendVC.SENT))
                    SmsManager.getDefault().sendTextMessage(number, null, message, PendingIntent.getBroadcast(this, 0, Intent(SendVC.SENT), 0), null)
                } catch(e: Throwable) {
                    e.printStackTrace()
                }
            } else {
                onError.invoke(SendVC.NEED_PERMISSION)
            }
        }
    }
}