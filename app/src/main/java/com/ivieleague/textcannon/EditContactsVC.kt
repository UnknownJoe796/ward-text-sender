package com.ivieleague.textcannon

import android.content.Intent
import android.text.InputType
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
import com.lightningkite.kotlin.anko.viewcontrollers.VCContext
import com.lightningkite.kotlin.anko.viewcontrollers.startIntent
import com.lightningkite.kotlin.async.invokeAsync
import com.lightningkite.kotlin.files.child
import com.lightningkite.kotlin.observable.list.ObservableList
import com.lightningkite.kotlin.observable.property.bind
import com.lightningkite.kotlin.observable.property.sub
import org.jetbrains.anko.*

/**
 * Created by josep on 5/8/2017.
 */
class EditContactsVC(val contacts: ObservableList<Contact>) : AnkoViewController() {
    override fun createView(ui: AnkoContext<VCContext>): View = ui.verticalLayout {
        linearLayout {
            padding = dip(8)

            textView {
                textResource = R.string.save_contacts_file
            }.lparams(0, wrapContent, 1f) { margin = dip(8) }

            imageButton {
                backgroundResource = selectableItemBackgroundBorderlessResource
                imageResource = R.drawable.ic_save_black_24dp
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                padding = dip(4)
                setOnClickListener { it: View? ->
                    ui.ctx.selector(
                            null,
                            R.string.overwrite_default_file to {
                                val copy = ArrayList<Contact>(contacts)
                                val doer = fun() {
                                    val file = ui.ctx.filesDir.child("main.csv")
                                    if (!file.exists()) file.createNewFile()
                                    file.writeBytes(ContactFile.write(copy))
                                }
                                doer.invokeAsync()
                            }
                    )
                }
            }.lparams(dip(32), dip(32)) { gravity = Gravity.RIGHT; margin = dip(8) }

            imageButton {
                backgroundResource = selectableItemBackgroundBorderlessResource
                imageResource = R.drawable.ic_share_black_24dp
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                padding = dip(4)
                setOnClickListener { it: View? ->
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_TEXT, ContactFile.writeString(contacts))
                        type = "text/csv"
                    }
                    ui.owner.startIntent(intent)
                }
            }.lparams(dip(32), dip(32)) { gravity = Gravity.RIGHT; margin = dip(8) }
        }
        linearLayout {
            padding = dip(8)
            textView {
                styleDefault()
                textResource = R.string.name
            }.lparams(0, wrapContent, 1f) { margin = dip(8) }
            textView {
                styleDefault()
                textResource = R.string.phone_number
            }.lparams(0, wrapContent, 1f) { margin = dip(8) }
            textView {
                styleDefault()
                textResource = R.string.tags
            }.lparams(0, wrapContent, 1f) { margin = dip(8) }
        }
        verticalRecyclerView {
            adapter = multiAdapter(
                    listAdapter(contacts) { itemObs ->
                        linearLayout {
                            padding = dip(8)

                            editText {
                                styleDefault()
                                hintResource = R.string.name
                                inputType = FullInputType.NAME
                                bindString(itemObs.sub(Contact::name))
                            }.lparams(0, wrapContent, 1f) { margin = dip(8) }

                            editText {
                                styleDefault()
                                hintResource = R.string.phone_number
                                inputType = FullInputType.PHONE
                                bindString(itemObs.sub(Contact::phoneNumber))
                            }.lparams(0, wrapContent, 1f) { margin = dip(8) }

                            editText {
                                styleDefault()
                                hintResource = R.string.tags
                                inputType = InputType.TYPE_CLASS_TEXT
                                lifecycle.bind(itemObs) { contact ->
                                    setText(contact.tags.joinToString())
                                }
                                textChangedListener {
                                    onTextChanged { value, _, _, _ ->
                                        itemObs.value.tags = value.toString().split(',').map { it.trim() }
                                    }
                                }
                            }.lparams(0, wrapContent, 1f) { margin = dip(8) }

                        }.lparams(matchParent, wrapContent)
                        Unit
                    },
                    singleAdapter {
                        textView {
                            styleDefault()
                            textResource = R.string.add_contact
                            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_add_black_24dp, 0, 0, 0)
                            backgroundResource = selectableItemBackgroundResource
                            setOnClickListener { it: View? ->
                                contacts.add(Contact("", "", listOf()))
                                scrollToPosition(adapter?.itemCount?.minus(1) ?: 0)
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