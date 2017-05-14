package com.ivieleague.textcannon

import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import com.lightningkite.kotlin.anko.*
import com.lightningkite.kotlin.anko.observable.bindString
import com.lightningkite.kotlin.anko.viewcontrollers.AnkoViewController
import com.lightningkite.kotlin.anko.viewcontrollers.containers.VCStack
import com.lightningkite.kotlin.anko.viewcontrollers.implementations.VCActivity
import com.lightningkite.kotlin.async.invokeAsync
import com.lightningkite.kotlin.files.child
import com.lightningkite.kotlin.observable.list.ObservableListWrapper
import com.lightningkite.kotlin.observable.list.filtering
import com.lightningkite.kotlin.observable.property.StandardObservableProperty
import com.lightningkite.kotlin.observable.property.bind
import com.lightningkite.kotlin.stream.toString
import org.jetbrains.anko.*
import org.jetbrains.anko.design.textInputLayout


class SendVC(val stack: VCStack) : AnkoViewController() {

    companion object {
        const val SENT = "SMS_SENT"
        const val NEED_PERMISSION = 23213
    }

    val message = StandardObservableProperty("")

    val contacts = ObservableListWrapper<Contact>()
    val tagsDesiredString = StandardObservableProperty("")
    val tagFilters = StandardObservableProperty(listOf<(Contact) -> Boolean>())
    val matchingContacts = contacts.filtering()

    init {
        tagsDesiredString += { query ->
            tagFilters.value = query.split(',').map { it.trim() }.filter { it.isNotBlank() }.map {
                val ands = it.split('+').map { it.trim() }.filter { it.isNotBlank() }
                return@map { contact: Contact -> ands.all { contact.tags.contains(it) } }
            }
        }
        tagFilters += { additiveFilters ->
            matchingContacts.filter = { contact ->
                if (additiveFilters.isEmpty()) true
                else additiveFilters.any { filter -> filter(contact) }
            }
        }
    }

    override fun createView(ui: AnkoContext<VCActivity>): View = ui.scrollView {

        if (contacts.isEmpty()) {
            val file = ui.owner.filesDir.child("main.csv")
            if (file.exists()) {
                {
                    val contacts = ContactFile.parse(file.readText())
                    contacts
                }.invokeAsync {
                    this@SendVC.contacts.replace(it)
                }
            }
        }

        verticalLayout {
            padding = dip(8)

            linearLayout {
                textView {
                    styleDefault()
                    lifecycle.bind(contacts.onUpdate) {
                        text = resources.getString(R.string.x_contacts_available, it.size)
                    }
                }.lparams(0, wrapContent, 1f) { margin = dip(8) }

                imageButton {
                    imageResource = R.mipmap.drive_icon
                    backgroundResource = selectableItemBackgroundBorderlessResource
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    padding = dip(4)
                    onClick {
                        ui.owner.getGoogleDriveFileInputStream(
                                types = listOf("text/csv"),
                                onCancel = { println("Canceled") },
                                onError = { println("Error: ${it.message}") },
                                onStreamObtained = {
                                    {
                                        val str = it.toString(Charsets.UTF_8)
                                        val contacts = ContactFile.parse(str)
                                        contacts
                                    }.invokeAsync {
                                        this@SendVC.contacts.replace(it)
                                    }
                                }
                        )
                    }
                }.lparams(dip(32), dip(32)) { margin = dip(8) }

                imageButton {
                    backgroundResource = selectableItemBackgroundBorderlessResource
                    imageResource = R.drawable.ic_folder_open_black_24dp
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    padding = dip(4)
                    onClick {
                        ui.owner.selector(
                                null,
                                R.string.open_default_file to {
                                    val file = ui.owner.filesDir.child("main.csv")
                                    if (file.exists()) {
                                        {
                                            val contacts = ContactFile.parse(file.readText())
                                            contacts
                                        }.invokeAsync {
                                            this@SendVC.contacts.replace(it)
                                        }
                                    }
                                }
                        )
                    }
                }.lparams(dip(32), dip(32)) { margin = dip(8) }

                imageButton {
                    imageResource = R.drawable.ic_edit_black_24dp
                    backgroundResource = selectableItemBackgroundBorderlessResource
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    padding = dip(4)
                    onClick {
                        stack.push(EditContactsVC(contacts))
                    }
                }.lparams(dip(32), dip(32)) { margin = dip(8) }
            }.lparams(matchParent, wrapContent)

            val showTagHint = StandardObservableProperty(false)
            textInputLayout {
                hintResource = R.string.tags
                textInputEditText {
                    styleDefault()
                    inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                    bindString(tagsDesiredString)
                    onFocusChange { view, b ->
                        showTagHint.value = b
                    }
                }
            }.lparams(matchParent, wrapContent) { margin = dip(8) }

            textView {
                styleDefault()
                maxLines = 3
                gravity = Gravity.CENTER
                textResource = R.string.tag_instructions
                lifecycle.bind(showTagHint) {
                    visibility = if (it) View.VISIBLE else View.GONE
                }
            }.lparams(matchParent, wrapContent) { margin = dip(8) }

            textView {
                styleDefault()
                gravity = Gravity.CENTER
                lifecycle.bind(contacts.onUpdate) {
                    text = resources.getString(
                            R.string.tags_available,
                            it.flatMap { it.tags }
                                    .map {
                                        it.toLowerCase()
                                    }
                                    .distinct()
                                    .joinToString()
                    )
                }
                lifecycle.bind(showTagHint) {
                    visibility = if (it) View.VISIBLE else View.GONE
                }
            }.lparams(matchParent, wrapContent) { margin = dip(8) }



            textInputLayout {
                hintResource = R.string.message
                textInputEditText {
                    styleDefault()
                    inputType = FullInputType.SENTENCE
                    maxLines = 20
                    bindString(message)
                }
            }.lparams(matchParent, wrapContent) { margin = dip(8) }

            button {
                lifecycle.bind(matchingContacts.onUpdate) {
                    text = resources.getString(R.string.review_sending_to_x_contacts, it.size)
                }
                onClick {
                    if (message.value.isBlank()) {
                        snackbar(R.string.validation_message_blank)
                        return@onClick
                    }
                    if (matchingContacts.isEmpty()) {
                        snackbar(R.string.validation_no_contacts)
                        return@onClick
                    }
                    stack.push(ConfirmVC(ArrayList(matchingContacts), message.value))
                }
            }.lparams(matchParent, wrapContent) { margin = dip(8) }

        }
    }
}