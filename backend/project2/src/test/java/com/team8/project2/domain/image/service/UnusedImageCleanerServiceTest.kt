package com.team8.project2.domain.image.service

import com.team8.project2.domain.image.entity.CurationImage
import com.team8.project2.domain.image.repository.CurationImageRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.util.List

@ExtendWith(MockitoExtension::class)
class UnusedImageCleanerServiceTest {
    @InjectMocks
    private val unusedImageCleanerService: UnusedImageCleanerService? = null

    @Mock
    private val curationImageRepository: CurationImageRepository? = null

    @Mock
    private val s3Uploader: S3Uploader? = null

    @Test
    @DisplayName("이미지 삭제 스케줄러에서 삭제 작업이 수행된다")
    fun testCleanUnusedImages() {
        val imageName = "imageName.png"

        `when`(curationImageRepository!!.findUnusedImages(any<LocalDateTime>()))
            .thenReturn(List.of(CurationImage(imageName)))

        // when: cleanUnusedImages 메서드 호출
        unusedImageCleanerService!!.cleanUnusedImages()

        // then: deleteUnusedImage 메서드가 호출되었는지 확인
        verify(curationImageRepository, times(1)).delete(
            ArgumentMatchers.any(
                CurationImage::class.java
            )
        )
        verify(s3Uploader, times(1))!!.deleteFile(imageName)
    }
}