/*
 * Create by Sungbin Ji on 2021. 1. 30.
 * Copyright (c) 2021. Sungbin Ji. All rights reserved. 
 *
 * SpakChat license is under the MIT license.
 * SEE LICENSE: https://github.com/sungbin5304/SpakChat/blob/master/LICENSE
 */

package me.sungbin.spakchat.ui.activity.chat

import android.graphics.Rect
import android.os.Bundle
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.StorageReference
import com.r0adkll.slidr.Slidr
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date
import javax.inject.Inject
import me.sungbin.androidutils.extensions.clear
import me.sungbin.androidutils.extensions.doDelay
import me.sungbin.androidutils.extensions.hide
import me.sungbin.androidutils.extensions.hideKeyboard
import me.sungbin.androidutils.extensions.setEndDrawableClickEvent
import me.sungbin.androidutils.extensions.setTint
import me.sungbin.androidutils.extensions.show
import me.sungbin.androidutils.extensions.showKeyboard
import me.sungbin.androidutils.extensions.toBottomScroll
import me.sungbin.androidutils.extensions.toColorStateList
import me.sungbin.spakchat.R
import me.sungbin.spakchat.SpakViewModel
import me.sungbin.spakchat.databinding.ActivityChatBinding
import me.sungbin.spakchat.di.Firestore
import me.sungbin.spakchat.di.RTDB
import me.sungbin.spakchat.di.Storage
import me.sungbin.spakchat.model.message.Message
import me.sungbin.spakchat.model.message.MessageType
import me.sungbin.spakchat.model.message.MessageViewType
import me.sungbin.spakchat.model.user.User
import me.sungbin.spakchat.ui.activity.BaseActivity
import me.sungbin.spakchat.util.KeyManager

@AndroidEntryPoint
class ChatActivity : BaseActivity() {

    @Firestore
    @Inject
    lateinit var firestore: FirebaseFirestore

    @Storage
    @Inject
    lateinit var storage: StorageReference

    @RTDB
    @Inject
    lateinit var database: DatabaseReference

    private val globalVM = SpakViewModel.instance()
    private val chatVM = ChatViewModel.instance()

    private var rootHeight = 0
    private var keyboardHeight = 0
    private var isEmoticonContainerShown = false

    private var _binding: ActivityChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var friend: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = DataBindingUtil.inflate(layoutInflater, R.layout.activity_chat, ConstraintLayout(this), false)
        setContentView(binding.root)

        val friendKey = intent.getLongExtra(KeyManager.User.KEY, -1)
        val databaseReference = database.child("chat/${globalVM.me.key}/friend/$friendKey")
        supportActionBar?.hide()
        Slidr.attach(this)
        friend = globalVM.users.find {
            it.key == friendKey
        }!!
        binding.user = friend

        databaseReference.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                chatVM.message.postValue(snapshot.getValue(Message::class.java))
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // todo: message edit
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // todo: message removed
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })

        val adapter = ChatAdapter(globalVM.me)
        binding.rvChat.adapter = adapter
        adapter.submit(chatVM.messagesMap[friendKey] ?: listOf())

        chatVM.message.observe(this) {
            chatVM.messagesMap[friendKey]?.add(it) ?: run {
                chatVM.messagesMap[friendKey] = mutableListOf(it)
            }
            adapter.submit(chatVM.messagesMap[friendKey]!!)
            binding.rvChat.toBottomScroll()
        }

        binding.ivBack.setOnClickListener { finish() }

        binding.etInput.doAfterTextChanged {
            if (it.toString().isNotBlank()) {
                binding.ivSend.setTint(
                    ContextCompat.getColor(
                        applicationContext,
                        R.color.colorPrimary
                    )
                )
            } else {
                binding.ivSend.setTint(
                    ContextCompat.getColor(
                        applicationContext,
                        R.color.colorTwiceLightGray
                    )
                )
            }
        }

        // https://wooooooak.github.io/android/2020/07/30/emoticon_container/
        binding.clContainer.viewTreeObserver.addOnGlobalLayoutListener {
            if (rootHeight == 0) rootHeight = binding.clContainer.height
            val visibleFrameSize = Rect()
            binding.clContainer.getWindowVisibleDisplayFrame(visibleFrameSize)
            val heightExceptKeyboard = visibleFrameSize.bottom - visibleFrameSize.top
            if (heightExceptKeyboard < rootHeight) {
                keyboardHeight = rootHeight - heightExceptKeyboard
            }
        }

        @Suppress("DEPRECATION")
        binding.etInput.setEndDrawableClickEvent {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
            if (!isEmoticonContainerShown) { // 컨테이너 보여주기
                TextViewCompat.setCompoundDrawableTintList(
                    binding.etInput,
                    getColor(R.color.colorGray).toColorStateList()
                )
                binding.tvTest.height = keyboardHeight
                binding.etInput.hideKeyboard()
                doDelay(50L) {
                    binding.tvTest.show()
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                }
            } else { // 컨테이너 가리기
                binding.etInput.showKeyboard()
                TextViewCompat.setCompoundDrawableTintList(
                    binding.etInput,
                    getColor(R.color.colorLightGray).toColorStateList()
                )
                doDelay(50L) {
                    binding.tvTest.hide(true)
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                }
            }
            isEmoticonContainerShown = !isEmoticonContainerShown
        }

        binding.ivSend.setOnClickListener {
            val inputMessage = binding.etInput.text.toString()
            if (inputMessage.isNotBlank()) {
                val message = Message(
                    key = globalVM.me.key,
                    message = inputMessage,
                    time = Date().time,
                    type = MessageType.CHAT,
                    attachment = null,
                    owner = globalVM.me,
                    mention = listOf(),
                    messageViewType = MessageViewType.NORMAL
                )
                databaseReference.push().setValue(message)
                binding.etInput.clear()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
