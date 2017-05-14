package com.ivieleague.textcannon

import android.view.Gravity
import android.view.View
import android.widget.ImageView
import com.lightningkite.kotlin.anko.*
import com.lightningkite.kotlin.anko.adapter.multiAdapter
import com.lightningkite.kotlin.anko.adapter.singleAdapter
import com.lightningkite.kotlin.anko.adapter.swipeToDismiss
import com.lightningkite.kotlin.anko.observable.adapter.listAdapter
import com.lightningkite.kotlin.anko.observable.bindString
import com.lightningkite.kotlin.anko.viewcontrollers.AnkoViewController
import com.lightningkite.kotlin.anko.viewcontrollers.implementations.VCActivity
import com.lightningkite.kotlin.async.invokeAsync
import com.lightningkite.kotlin.files.child
import com.lightningkite.kotlin.observable.list.ObservableList
import com.lightningkite.kotlin.observable.property.bind
import com.lightningkite.kotlin.observable.property.sub
import org.jetbrains.anko.*
import org.jetbrains.anko.design.textInputLayout

/**
 * Created by josep on 5/8/2017.
 */
class EditContactsVC(val contacts: ObservableList<Contact>) : AnkoViewController() {
    override fun createView(ui: AnkoContext<VCActivity>): View = ui.verticalLayout {
        linearLayout {
            padding = dip(8)
            textView {
                textResource = R.string.save_contacts_file

            }.lparams(0, wrapContent, 1f) { margin = dip(8) }
            imageButton {
                backgroundResource = selectableItemBackgroundBorderlessResource
                imageResource = R.mipmap.drive_icon
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                padding = dip(4)
                onClick {
                    ui.owner.selector(
                            null,
                            //                            R.string.new_file to {
//                                ui.owner.newGoogleDriveFile(
//                                        resources.getString(R.string.save_contacts_file),
//                                        ContactFile.write(contacts),
//                                        "text/csv",
//                                        onCancel = { println("Canceled") },
//                                        onError = { println("On Error $it") },
//                                        onDone = { println("Success") }
//                                )
//                            },
                            R.string.overwrite_file to {
                                ui.owner.getGoogleDriveFileOverwrite(
                                        ContactFile.write(contacts),
                                        listOf("text/csv"),
                                        onCancel = { println("Canceled") },
                                        onError = { println("On Error $it") },
                                        onDone = { println("Success") }
                                )
                            }
                    )
                }
            }.lparams(dip(32), dip(32)) { gravity = Gravity.RIGHT; margin = dip(8) }
            imageButton {
                backgroundResource = selectableItemBackgroundBorderlessResource
                imageResource = R.drawable.ic_save_black_24dp
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                padding = dip(4)
                onClick {
                    ui.owner.selector(
                            null,
                            R.string.overwrite_default_file to {
                                val copy = ArrayList<Contact>(contacts)
                                val doer = fun() {
                                    val file = ui.owner.filesDir.child("main.csv")
                                    if (!file.exists()) file.createNewFile()
                                    file.writeBytes(ContactFile.write(copy))
                                }
                                doer.invokeAsync()
                            }
                    )
                }
            }.lparams(dip(32), dip(32)) { gravity = Gravity.RIGHT; margin = dip(8) }
        }
        verticalRecyclerView {
            adapter = multiAdapter(
                    listAdapter(contacts) { itemObs ->
                        linearLayout {
                            padding = dip(8)

                            textInputLayout {
                                hintResource = R.string.name
                                textInputEditText {
                                    bindString(itemObs.sub(Contact::name))
                                }
                            }.lparams(0, wrapContent, 1f)

                            textInputLayout {
                                hintResource = R.string.phone_number
                                textInputEditText {
                                    bindString(itemObs.sub(Contact::phoneNumber))
                                }
                            }.lparams(0, wrapContent, 1f)

                            textInputLayout {
                                hintResource = R.string.tags
                                textInputEditText {
                                    lifecycle.bind(itemObs) { contact ->
                                        setText(contact.tags.joinToString())
                                    }
                                    textChangedListener {
                                        onTextChanged { value, _, _, _ ->
                                            itemObs.value.tags = value.toString().split(',').map { it.trim() }
                                        }
                                    }
                                }
                            }.lparams(0, wrapContent, 1f)

                        }.lparams(matchParent, wrapContent)
                        Unit
                    },
                    singleAdapter {
                        textView {
                            styleDefault()
                            textResource = R.string.add_contact
                            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_add_black_24dp, 0, 0, 0)
                            backgroundResource = selectableItemBackgroundResource
                            onClick {
                                contacts.add(Contact("", "", listOf()))
                            }
                        }
                    }
            )

            swipeToDismiss(
                    canDismiss = { it in contacts.indices },
                    action = { contacts.removeAt(it) }
            )
        }
    }
}