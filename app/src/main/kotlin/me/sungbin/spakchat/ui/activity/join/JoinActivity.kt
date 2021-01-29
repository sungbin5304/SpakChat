/*
 * Create by Sungbin Ji on 2021. 1. 30.
 * Copyright (c) 2021. Sungbin Ji. All rights reserved.
 *
 * SpakChat license is under the MIT license.
 * SEE LICENSE : https://github.com/sungbin5304/SpakChat/blob/master/LICENSE
 */

package me.sungbin.spakchat.ui.activity.join

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.WindowManager
import androidx.activity.viewModels
import com.sangcomz.fishbun.FishBun
import dagger.hilt.android.AndroidEntryPoint
import me.sungbin.androidutils.extensions.get
import me.sungbin.androidutils.tagableroundimageview.TagableRoundImageView
import me.sungbin.spakchat.R
import me.sungbin.spakchat.SpakViewModel
import me.sungbin.spakchat.databinding.ActivityJoinBinding
import me.sungbin.spakchat.module.GlideApp
import me.sungbin.spakchat.ui.activity.BaseActivity

@AndroidEntryPoint
class JoinActivity : BaseActivity() {

    private val vm: SpakViewModel by viewModels()
    private val signupBottomDialog by lazy { RegisterBottomDialog.instance(vm) }
    private val signinBottomDialog by lazy { LoginBottomDialog.instance(vm) }

    private val binding by lazy { ActivityJoinBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        binding.btnSignup.setOnClickListener {
            signupBottomDialog.show(supportFragmentManager, "")
        }

        binding.btnSignin.setOnClickListener {
            signinBottomDialog.show(supportFragmentManager, "")
        }

        binding.tvDescription.run {
            val ssb = SpannableStringBuilder(text)
            ssb.setSpan(
                StyleSpan(Typeface.BOLD),
                text.lastIndexOf("새로운"),
                text.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            ssb.setSpan(
                RelativeSizeSpan(1.6f),
                text.lastIndexOf("새로운"),
                text.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            text = ssb
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            FishBun.FISHBUN_REQUEST_CODE -> {
                val profileImageView =
                    signupBottomDialog.view?.get(
                        R.id.iv_profile,
                        TagableRoundImageView::class.java
                    )
                val uri =
                    data?.getParcelableArrayListExtra<Uri>(FishBun.INTENT_PATH)?.get(0) ?: return
                profileImageView!!.setPadding(0, 0, 0, 0)
                GlideApp.with(applicationContext).load(uri).into(profileImageView)
                profileImageView.tag = uri
            }
        }
    }
}