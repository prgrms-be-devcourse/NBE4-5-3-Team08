package com.team8.project2.domain.image.service

import com.team8.project2.domain.image.repository.CurationImageRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class UnusedImageCleanerService(
    private val curationImageRepository: CurationImageRepository,
    private val s3Uploader: S3Uploader
) {

    // 매일 오전 5시에 실행
    @Transactional
    @Scheduled(cron = "0 0 5 * * *")
    fun cleanUnusedImages() {
        val cutoffDate = LocalDateTime.now().minus(1, ChronoUnit.DAYS)

        val unusedImages = curationImageRepository.findUnusedImages(cutoffDate)
        for (unusedImage in unusedImages) {
            s3Uploader.deleteFile(unusedImage.imageName)
            curationImageRepository.delete(unusedImage)
        }
    }
}
