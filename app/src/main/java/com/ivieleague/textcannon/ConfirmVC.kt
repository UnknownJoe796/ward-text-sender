package com.ivieleague.textcannon

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
import android.util.Log
import android.view.View
import com.lightningkite.kotlin.anko.viewcontrollers.AnkoViewController
import com.lightningkite.kotlin.anko.viewcontrollers.VCContext
import org.jetbrains.anko.*

/**
 * Created by josep on 5/14/2017.
 */
class ConfirmVC(val contacts: List<Contact>, val message: String, val onSend: () -> Unit) : AnkoViewController() {

    override fun getTitle(resources: Resources): String = resources.getString(R.string.confirm)

    override fun createView(ui: AnkoContext<VCContext>): View = ui.scrollView {
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

                setOnClickListener { it: View? ->

                    val notificationBuilder = NotificationCompat.Builder(context)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentTitle("Sending texts...")
                            .setContentText("0 / ${contacts.size}")
                            .setPriority(NotificationCompat.PRIORITY_LOW)
                            .setProgress(contacts.size, 0, false)
                    ui.owner.sendSmsList(
                            contacts = contacts,
                            message = message,
                            onProgress = { index, errors ->
                                notificationBuilder.setContentText("$index / ${contacts.size}")
                                notificationBuilder.setProgress(contacts.size, index, false)
                                context.notificationManager.notify(0, notificationBuilder.build())
                            },
                            onComplete = { errors ->
                                if (errors == 0) {
                                    notificationBuilder.setContentTitle("Texts sent.")
                                } else {
                                    notificationBuilder.setContentTitle("Texts sent with $errors errors.")
                                }
                                notificationBuilder.priority = NotificationCompat.PRIORITY_DEFAULT
                                notificationBuilder.setContentText("Complete")
                                notificationBuilder.setProgress(contacts.size, contacts.size, false)
                                context.notificationManager.notify(0, notificationBuilder.build())
                            }
                    )

                }
            }
        }
    }

    fun VCContext.sendSmsList(contacts: List<Contact>, message: String, onProgress: (count: Int, errorCount: Int) -> Unit, onComplete: (errorCount: Int) -> Unit) {
        var index = 0
        var errorCount = 0
        fun sendNext() {
            if (index >= contacts.size) {
                onComplete.invoke(errorCount)
            } else {
                onProgress.invoke(index, errorCount)
                val brokenMessage = SmsManager.getDefault().divideMessage(message.replace("\$NAME", contacts[index].name))
                sendSms(contacts[index].phoneNumber, brokenMessage, onError = { errorCount++ }, onComplete = {
                    sendNext()
                })
            }
            index++
        }
        if (contacts.isNotEmpty()) {
            sendNext()
        }
    }

    fun VCContext.sendSms(number: String, brokenMessage: ArrayList<String>, onError: (code: Int) -> Unit, onComplete: () -> Unit) {
        val appContext = context.applicationContext
        requestPermission(Manifest.permission.SEND_SMS) {
            if (it) {
                Log.i("sendSMS", "Sending")
                onSend()
                try {
                    var partsThrough = 0
                    var errorCode: Int? = null
                    appContext.registerReceiver(object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent?) {
                            val part = intent?.getIntExtra("part", -1)
                            partsThrough++
                            when (resultCode) {
                                Activity.RESULT_OK -> {
                                    Log.i("sendSMS", "Success part $part / $partsThrough")
                                }
                                else -> {
                                    Log.e("sendSMS", "Error part $part / $partsThrough: $resultCode")
                                    errorCode = resultCode
                                }
                            }
                            if (partsThrough == brokenMessage.size) {
                                Log.i("sendSMS", "Finished with this text")
                                appContext.unregisterReceiver(this)
                                if (errorCode == null)
                                    onComplete.invoke()
                                else
                                    onError.invoke(errorCode!!)
                            }
                        }
                    }, IntentFilter(SendVC.SENT))
                    SmsManager.getDefault().sendMultipartTextMessage(
                            number,
                            null,
                            brokenMessage,
                            ArrayList(Array<PendingIntent>(brokenMessage.size) {
                                PendingIntent.getBroadcast(
                                        context,
                                        0,
                                        Intent(SendVC.SENT).apply {
                                            putExtra("part", it)
                                        },
                                        0
                                )
                            }.toList()),
                            null
                    )
                } catch(e: Throwable) {
                    e.printStackTrace()
                }
            } else {
                onError.invoke(SendVC.NEED_PERMISSION)
            }
        }
    }
}