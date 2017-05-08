package com.ivieleague.wardtextsender

import android.text.InputType
import android.view.Gravity
import android.view.View
import com.lightningkite.kotlin.anko.FullInputType
import com.lightningkite.kotlin.anko.lifecycle
import com.lightningkite.kotlin.anko.observable.bindString
import com.lightningkite.kotlin.anko.viewcontrollers.AnkoViewController
import com.lightningkite.kotlin.anko.viewcontrollers.implementations.VCActivity
import com.lightningkite.kotlin.async.invokeAsync
import com.lightningkite.kotlin.observable.list.ObservableListWrapper
import com.lightningkite.kotlin.observable.list.filtering
import com.lightningkite.kotlin.observable.property.StandardObservableProperty
import com.lightningkite.kotlin.observable.property.bind
import com.lightningkite.kotlin.stream.toString
import org.jetbrains.anko.*


class MainVC : AnkoViewController() {

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
            println(tagFilters.value)
        }
        tagFilters += { additiveFilters ->
            matchingContacts.filter = { contact ->
                if (additiveFilters.isEmpty()) true
                else additiveFilters.any { filter -> filter(contact) }
            }
        }
    }

    override fun createView(ui: AnkoContext<VCActivity>): View = ui.scrollView {
        verticalLayout {
            padding = dip(8)

            textView {
                styleHeader()
                gravity = Gravity.CENTER
                textResource = R.string.ward_text_sender
            }.lparams(matchParent, wrapContent) { margin = dip(8) }

            button {
                textResource = R.string.open_contacts_from_google_drive
                onClick {
                    ui.owner.getGoogleDriveFileInputStream(
                            types = listOf("text/csv"),
                            onCancel = { println("Canceled") },
                            onError = { println("Error: ${it.message}") },
                            onDone = {
                                {
                                    val str = it.toString(Charsets.UTF_8)
                                    println(str)
                                    val contacts = ContactFile.parse(str)
                                    println(contacts)
                                    contacts
                                }.invokeAsync {
                                    this@MainVC.contacts.addAll(it)
                                    println(this@MainVC.contacts)
                                }
                            }
                    )
                }
            }.lparams(matchParent, wrapContent) { margin = dip(8) }

            textView {
                styleDefault()
                gravity = Gravity.CENTER
                lifecycle.bind(contacts.onUpdate) {
                    text = resources.getString(R.string.x_contacts_available, it.size)
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
            }.lparams(matchParent, wrapContent) { margin = dip(8) }

            textView {
                styleDefault()
                maxLines = 3
                gravity = Gravity.CENTER
                textResource = R.string.tag_instructions
            }.lparams(matchParent, wrapContent) { margin = dip(8) }

            editText {
                styleDefault()
                inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                bindString(tagsDesiredString)
            }.lparams(matchParent, wrapContent) { margin = dip(8) }

            textView {
                styleDefault()
                gravity = Gravity.CENTER
                lifecycle.bind(matchingContacts.onUpdate) {
                    text = resources.getString(R.string.x_contacts_match, it.size)
                }
            }.lparams(matchParent, wrapContent) { margin = dip(8) }

            editText {
                styleDefault()
                inputType = FullInputType.SENTENCE
                maxLines = 20
                bindString(message)
            }.lparams(matchParent, wrapContent) { margin = dip(8) }

            button {
                textResource = R.string.send
                onClick {
                    //TODO

                }
            }.lparams(matchParent, wrapContent) { margin = dip(8) }

        }
    }
}