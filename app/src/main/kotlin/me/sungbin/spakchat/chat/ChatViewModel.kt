/*
 * Create by Sungbin Ji on 2021. 2. 6.
 * Copyright (c) 2021. Sungbin Ji. All rights reserved.
 *
 * SpakChat license is under the MIT license.
 * SEE LICENSE: https://github.com/sungbin5304/SpakChat/blob/master/LICENSE
 */

package me.sungbin.spakchat.chat

import androidx.lifecycle.ViewModel
import me.sungbin.spakchat.room.Room

class ChatViewModel private constructor() : ViewModel() {

    val openRooms = mutableListOf<Room>()
    val friendsRooms = mutableListOf<Room>()
    val messagesMap = HashMap<Long, MutableList<me.sungbin.spakchat.chat.model.Chat>>()

    companion object {
        private val instance = ChatViewModel()
        fun instance() = instance
    }
}
