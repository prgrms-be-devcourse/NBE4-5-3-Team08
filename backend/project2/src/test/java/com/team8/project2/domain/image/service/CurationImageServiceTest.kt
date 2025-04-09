package com.team8.project2.domain.image.service

import com.team8.project2.domain.image.repository.CurationImageRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.mock.web.MockMultipartFile
import java.io.IOException

@ExtendWith(MockitoExtension::class)
internal class CurationImageServiceTest {
    @InjectMocks
    private val curationImageService: CurationImageService? = null

    @Mock
    private val curationImageRepository: CurationImageRepository? = null

    @Mock
    private val s3Uploader: S3Uploader? = null

    @Test
    @DisplayName("큐레이션 작성 중 이미지를 업로드하면 이미지 정보가 저장되고 S3에 업로드된다")
    @Throws(IOException::class)
    fun uploadImage() {
        val mockFile = MockMultipartFile(
            "file",
            "test-image.jpg",
            "image/jpeg",
            "test-image-content".toByteArray()
        )
        Mockito.`when`(s3Uploader!!.uploadFile(mockFile)).thenReturn("saved-image.webp")

        curationImageService!!.uploadImage(mockFile)

        Mockito.verify(s3Uploader).uploadFile(mockFile)
        Mockito.verify(curationImageRepository)!!.save(ArgumentMatchers.any())
    }

    @Test
    @DisplayName("큐레이션 ID로 큐레이션에 첨부된 이미지를 찾을 수 있다")
    fun findByCurationId() {
        val curationId = 1L
        curationImageService!!.findByCurationId(curationId)
        Mockito.verify(curationImageRepository)!!.findByCurationId(curationId)
    }

    @Test
    @DisplayName("이미지명으로 큐레이션에 첨부된 이미지 정보를 삭제할 수 있다")
    fun deleteByImageName() {
        val imageName = "test-image.jpg"
        curationImageService!!.deleteByImageName(imageName)
        Mockito.verify(curationImageRepository)!!.deleteByImageName(imageName)
    }

    @Test
    @DisplayName("큐레이션 ID로 큐레이션에 첨부된 모든 이미지 정보를 삭제할 수 있다")
    fun deleteByCurationId() {
        val curationId = 1L
        curationImageService!!.deleteByCurationId(curationId)
        Mockito.verify(curationImageRepository)!!.deleteByCurationId(curationId)
    }
}