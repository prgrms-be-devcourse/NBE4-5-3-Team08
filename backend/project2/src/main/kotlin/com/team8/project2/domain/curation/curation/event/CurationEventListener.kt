package com.team8.project2.domain.curation.curation.event

import com.team8.project2.domain.image.service.CurationImageService
import com.team8.project2.domain.image.service.S3Uploader
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class CurationEventListener(
    private val curationImageService: CurationImageService,
    private val s3Uploader: S3Uploader
) {

    /**
     * 큐레이션이 수정되었을 때, 큐레이션 내용에서 삭제된 이미지를 DB와 S3에서 삭제 처리한다
     * @param event 큐레이션 수정 이벤트
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun deleteImageForUpdatedCuration(event: CurationUpdateEvent) {
        val curationId = event.curationId
        val imageUrls = event.imageUrls

        val savedImages = curationImageService.findByCurationId(curationId).toMutableList()
        savedImages.removeIf { it.imageName in imageUrls }

        for (savedImage in savedImages) {
            s3Uploader.deleteFile(savedImage.imageName)
            curationImageService.deleteByImageName(savedImage.imageName)
        }
    }

    /**
     * 큐레이션이 삭제되었을 때, 연결된 이미지를 DB와 S3에서 삭제 처리한다
     * @param event 큐레이션 삭제 이벤트
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun deleteImageForDeletedCuration(event: CurationDeleteEvent) {
        val curationId = event.curationId
        val savedImages = curationImageService.findByCurationId(curationId)

        for (savedImage in savedImages) {
            s3Uploader.deleteFile(savedImage.imageName)
        }

        curationImageService.deleteByCurationId(curationId)
    }
}
