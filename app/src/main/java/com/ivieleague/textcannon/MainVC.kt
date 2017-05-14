package com.ivieleague.textcannon

import android.view.Gravity
import android.view.View
import com.lightningkite.kotlin.anko.selectableItemBackgroundBorderlessResource
import com.lightningkite.kotlin.anko.viewcontrollers.AnkoViewController
import com.lightningkite.kotlin.anko.viewcontrollers.containers.VCStack
import com.lightningkite.kotlin.anko.viewcontrollers.implementations.VCActivity
import org.jetbrains.anko.*
import org.jetbrains.anko.design.appBarLayout

class MainVC : AnkoViewController() {
    val stack = VCStack().apply { push(SendVC(this)) }

    override fun createView(ui: AnkoContext<VCActivity>): View = ui.verticalLayout {

        appBarLayout {
            linearLayout {
                gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
                imageButton {
                    backgroundResource = selectableItemBackgroundBorderlessResource
                    imageResource = R.drawable.ic_arrow_back_white_24dp
                    alpha = 0f
                    stack.onSwap += {
                        if (stack.size <= 1)
                            animate().alpha(0f).setDuration(100).start()
                        else
                            animate().alpha(1f).setDuration(100).start()
                    }
                    onClick {
                        if (stack.size > 1) {
                            stack.pop()
                        }
                    }
                }.lparams(wrapContent, wrapContent) { margin = dip(8) }

                textView {
                    styleHeader()
                    padding = dip(8)
                    textResource = R.string.app_name
                }.lparams(wrapContent, wrapContent) { margin = dip(8) }
            }.lparams(matchParent, wrapContent)
        }.lparams(matchParent, wrapContent)

        viewContainer(stack).lparams(matchParent, 0, 1f)
    }

    override fun onBackPressed(backAction: () -> Unit) {
        stack.onBackPressed(backAction)
    }
}