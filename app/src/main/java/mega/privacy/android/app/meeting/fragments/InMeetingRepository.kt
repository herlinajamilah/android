package mega.privacy.android.app.meeting.fragments

import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.lollipop.controllers.ChatController
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.listeners.*
import mega.privacy.android.app.utils.*
import nz.mega.sdk.*
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMeetingRepository @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    @ApplicationContext private val context: Context
) {

    /**
     * Get the initial name of the meeting created
     *
     * @return String meeting's name
     */
    fun getInitialMeetingName(): String {
        return StringResourcesUtils.getString(
            R.string.type_meeting_name, megaChatApi.myFullname
        )
    }

    /**
     * Set a title for a chat
     *
     * @param chatId chat ID
     * @param newTitle new chat title
     * @param listener MegaChatRequestListenerInterface
     */
    fun setTitleChatRoom(
        chatId: Long,
        newTitle: String,
        listener: MegaChatRequestListenerInterface
    ) {
        megaChatApi.setChatTitle(chatId, newTitle, listener)
    }

    /**
     * Method for starting a meeting
     *
     * @param chatId chat ID
     * @param enableAudio if Audio is enabled
     * @param enableVideo if Video is enabled
     * @param listener MegaChatRequestListenerInterface
     */
    fun startCall(
        chatId: Long,
        enableAudio: Boolean,
        enableVideo: Boolean,
        listener: MegaChatRequestListenerInterface
    ) {
        megaChatApi.startChatCall(chatId, enableVideo, enableAudio, listener)
    }

    /**
     * Method for starting a meeting
     *
     * @param chatId chat ID
     * @param enableAudio if Audio is enabled
     * @param enableVideo if Video is enabled
     * @param listener MegaChatRequestListenerInterface
     */
    fun answerCall(
        chatId: Long,
        enableAudio: Boolean,
        enableVideo: Boolean,
        listener: MegaChatRequestListenerInterface
    ) {
        megaChatApi.answerChatCall(chatId, enableVideo, enableAudio, listener)
    }

    /**
     * Get a call from a chat id
     *
     * @param chatId chat ID
     * @return MegaChatCall
     */
    fun getMeeting(chatId: Long): MegaChatCall? {
        if (chatId == MEGACHAT_INVALID_HANDLE)
            return null

        return megaChatApi.getChatCall(chatId)
    }

    /**
     * Method to know if it's me
     *
     * @param peerId The handle
     * @return True, if it's me. False, otherwise
     */
    fun isMe(peerId: Long?): Boolean {
        return peerId == megaChatApi.myUserHandle
    }


    /**
     * Get the session in a one to one call
     *
     * @param call The MegaChatCall
     * @return MegaChatSession
     */
    fun getSessionOneToOneCall(call: MegaChatCall): MegaChatSession? {
        val clientId = call.sessionsClientid?.get(0)
        clientId?.let {
            return call.getMegaChatSession(it)
        }

        return null
    }

    /**
     * Get a chat from a chat id
     *
     * @param chatId chat ID
     * @return MegaChatRoom
     */
    fun getChatRoom(chatId: Long): MegaChatRoom? {
        if (chatId == MEGACHAT_INVALID_HANDLE)
            return null

        return megaChatApi.getChatRoom(chatId)
    }

    /**
     * Get contact name
     *
     * @param peerId contact handle
     * @return String The contact's name
     */
    fun getContactOneToOneCallName(peerId: Long): String {
        val name: String =
            ChatController(MegaApplication.getInstance().applicationContext).getParticipantFirstName(
                peerId
            )
        if (TextUtil.isTextEmpty(name)) {
            return megaChatApi.getContactEmail(peerId)
        }

        return name
    }

    /**
     * Method for creating a meeting
     *
     * @param meetingName the name of the meeting
     * @param listener MegaChatRequestListenerInterface
     */
    fun createMeeting(meetingName: String, listener: MegaChatRequestListenerInterface) {
        megaChatApi.createMeeting(meetingName, listener)
    }

    /**
     * Method to switch a call on hold
     *
     * @param chatId chat ID
     * @param isHold True, if I am going to put it on hold. False, otherwise
     */
    fun setCallOnHold(chatId: Long, isOn: Boolean) {
        if (chatId != MEGACHAT_INVALID_HANDLE) {
            megaChatApi.setCallOnHold(chatId, isOn, SetCallOnHoldListener(context))
        }
    }

    /**
     * Method for ignore a call
     *
     * @param chatId chat ID
     */
    fun ignoreCall(chatId: Long) {
        if (chatId == MEGACHAT_INVALID_HANDLE)
            return

        megaChatApi.setIgnoredCall(chatId)
        MegaApplication.getInstance().stopSounds()
        CallUtil.clearIncomingCallNotification(chatId)
    }

    /**
     * Method for leave a meeting
     *
     * @param chatId chat ID
     */
    fun leaveMeeting(callId: Long) {
        if (callId == MEGACHAT_INVALID_HANDLE)
            return

        megaChatApi.hangChatCall(callId, HangChatCallListener(context))
    }

    fun getMyAvatar(chat: MegaChatRoom): Bitmap? {
        var avatar = CallUtil.getImageAvatarCall(chat, megaChatApi.myUserHandle)
        when (avatar) {
            null -> {
                avatar = CallUtil.getDefaultAvatarCall(
                    MegaApplication.getInstance().applicationContext,
                    megaChatApi.myUserHandle
                )
            }
        }
        return avatar
    }

    /**
     * Get the avatar
     *
     * @param chat
     * @param peerId
     * @return the avatar
     */
    fun getAvatarBitmap(chat: MegaChatRoom, peerId: Long): Bitmap? {
        var avatar = CallUtil.getImageAvatarCall(chat, peerId)
        if (avatar == null) {
            avatar = CallUtil.getDefaultAvatarCall(
                MegaApplication.getInstance().applicationContext,
                peerId
            )
        }

        return avatar
    }

    /**
     * Create a participant with my data
     *
     * @param chat MegaChatRoom
     * @return me as a participant
     */
    fun getMeToSpeakerView(chat: MegaChatRoom): Participant {
        var isAudioOn = true
        var isVideoOn = true

        getMeeting(chat.chatId)?.let {
            isAudioOn = it.hasLocalAudio()
            isVideoOn = it.hasLocalVideo()
        }

        val avatar = getAvatarBitmap(chat, megaChatApi.myUserHandle)
        return Participant(
            megaChatApi.myUserHandle,
            MEGACHAT_INVALID_HANDLE,
            megaChatApi.myFullname,
            avatar,
            true,
            getOwnPrivileges(chat.chatId) == MegaChatRoom.PRIV_MODERATOR,
            isAudioOn,
            isVideoOn,
            false,
            true,
            true,
            null
        )
    }

    /**
     * Method to know if a user is my contact
     *
     * @param peerId
     * @return True, if it's. False, otherwise.
     */
    fun isMyContact(peerId: Long): Boolean {
        val email: String = ChatController(context).getParticipantEmail(peerId)
        val contact = megaApi.getContact(email)

        return contact != null && contact.visibility == MegaUser.VISIBILITY_VISIBLE
    }

    /**
     * Method to get the participant's name
     *
     * @param peerId
     * @return The name
     */
    fun participantName(peerId: Long): String {
        return ChatController(context).getParticipantFullName(peerId)
    }

    /**
     * Method of obtaining the remote video
     *
     * @param chatId chatId
     * @param clientId client ID
     * @param hiRes If it's has High resolution
     * @param listener GroupVideoListener
     */
    fun addRemoteVideo(
        chatId: Long,
        clientId: Long,
        hiRes: Boolean,
        listener: GroupVideoListener
    ) {
        megaChatApi.addChatRemoteVideoListener(chatId, clientId, hiRes, listener)
    }

    /**
     * Method of obtaining the remote video
     *
     * @param chatId chatId
     * @param clientId client ID
     * @param hiRes If it's has High resolution
     * @param listener MeetingVideoListener
     */
    fun addRemoteVideoOneToOneCall(
        chatId: Long,
        clientId: Long,
        hiRes: Boolean,
        listener: MeetingVideoListener
    ) {
        megaChatApi.addChatRemoteVideoListener(chatId, clientId, hiRes, listener)
    }

    /**
     * Method of remove the remote video
     *
     * @param chatId chatId
     * @param clientId client ID
     * @param hiRes If it's has High resolution
     * @param listener GroupVideoListener
     */
    fun removeRemoteVideo(
        chatId: Long,
        clientId: Long,
        hiRes: Boolean,
        listener: GroupVideoListener
    ) {
        megaChatApi.removeChatVideoListener(chatId, clientId, hiRes, listener)
    }

    /**
     * Method of remove the remote video
     *
     * @param chatId chatId
     * @param clientId client ID
     * @param hiRes If it's has High resolution
     * @param listener MeetingVideoListener
     */
    fun removeRemoteVideoOneToOneCall(
        chatId: Long,
        clientId: Long,
        hiRes: Boolean,
        listener: MeetingVideoListener
    ) {
        megaChatApi.removeChatVideoListener(chatId, clientId, hiRes, listener)
    }

    fun requestHiResVideo(
        chatId: Long,
        clientId: Long,
        listener: MegaChatRequestListenerInterface
    ) {
        megaChatApi.requestHiResVideo(chatId, clientId, listener)
    }

    fun stopHiResVideo(chatId: Long, clientId: MegaHandleList, listener: MegaChatRequestListenerInterface) {
        megaChatApi.stopHiResVideo(chatId, clientId, listener)
    }

    fun requestLowResVideo(
        chatId: Long,
        clientId: MegaHandleList,
        listener: MegaChatRequestListenerInterface
    ) {
        megaChatApi.requestLowResVideo(chatId, clientId, listener)

    }

    fun stopLowResVideo(
        chatId: Long,
        clientId: MegaHandleList,
        listener: MegaChatRequestListenerInterface
    ) {
        megaChatApi.stopLowResVideo(chatId, clientId, listener)

    }

    /**
     * Method of obtaining the local video
     *
     * @param chatId chatId
     * @param listener GroupVideoListener
     */
    fun addLocalVideoSpeaker(chatId: Long, listener: GroupVideoListener) {
        megaChatApi.addChatLocalVideoListener(chatId, listener)
    }

    /**
     * Method of remove the local video
     *
     * @param chatId chatId
     * @param listener GroupVideoListener
     */
    fun removeLocalVideoSpeaker(chatId: Long, listener: GroupVideoListener) {
        megaChatApi.removeChatVideoListener(chatId, MEGACHAT_INVALID_HANDLE, false, listener)
    }

    /**
     * Method to get own privileges in a chat
     *
     * @param chatId
     * @return the privileges
     */
    fun getOwnPrivileges(chatId: Long): Int {
        getChatRoom(chatId)?.let {
            return it.ownPrivilege
        }

        return -1
    }

    fun openChatPreview(link: String, listener: MegaChatRequestListenerInterface) =
        megaChatApi.openChatPreview(link, listener)

    fun joinPublicChat(chatId: Long, listener: MegaChatRequestListenerInterface) =
        megaChatApi.autojoinPublicChat(chatId, listener)

    fun createEphemeralAccountPlusPlus(
        firstName: String,
        lastName: String,
        listener: MegaRequestListenerInterface
    ) {
        megaApi.createEphemeralAccountPlusPlus(firstName, lastName, listener)
    }

    fun getMyInfo(moderator: Boolean, audio: Boolean, video: Boolean): Participant {
        return Participant(
            megaChatApi.myUserHandle,
            MEGACHAT_INVALID_HANDLE,
            megaChatApi.myFullname ?: "",
            null, true, moderator, audio, video
        )
    }

    fun updateChatPermissions(chatId: Long, peerId: Long,
            listener: MegaChatRequestListenerInterface?){
        megaChatApi.updateChatPermissions(
            chatId,
            peerId,
            MegaChatRoom.PRIV_MODERATOR,
            listener
        )
    }

    fun getAvatarBitmapByPeerId(peerId: Long): Bitmap? {
        val mail = ChatController(context).getParticipantEmail(
                peerId
            )
        val userHandleString = MegaApiAndroid.userHandleToBase64(peerId)
        val myUserHandleEncoded = MegaApiAndroid.userHandleToBase64(megaChatApi.myUserHandle)
        if (userHandleString == myUserHandleEncoded) {
            return AvatarUtil.getAvatarBitmap(mail)
        }
        return if (TextUtil.isTextEmpty(mail)) AvatarUtil.getAvatarBitmap(userHandleString) else AvatarUtil.getUserAvatar(
            userHandleString,
            mail
        )
    }
}