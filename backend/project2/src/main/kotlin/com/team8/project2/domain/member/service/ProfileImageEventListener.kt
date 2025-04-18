package com.team8.project2.domain.member.service

import com.team8.project2.domain.image.service.S3Uploader
import com.team8.project2.domain.member.event.ProfileImageUpdateEvent
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionalEventListener

@Slf4j
@Component
@RequiredArgsConstructor
class ProfileImageEventListener {
    private val s3Uploader: S3Uploader? = null

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener
    fun deleteOldProfileImage(event: ProfileImageUpdateEvent) {
        val oldProfileImageName = extractFileNameFromUrl(event.oldProfileImageUrl)
        if (isDefaultProfile(oldProfileImageName)) {
            return
        }
        s3Uploader!!.deleteFile(oldProfileImageName)
    }

    private fun isDefaultProfile(oldProfileImageName: String): Boolean {
        return (oldProfileImageName == DEAFULT_PROFILE_IMAGE_NAME)
    }

    private fun extractFileNameFromUrl(fileUrl: String): String {
        return fileUrl.substring(fileUrl.lastIndexOf("/") + 1)
    }

    companion object {
        private const val DEAFULT_PROFILE_IMAGE_NAME = "default-profile.svg"
    }
}
