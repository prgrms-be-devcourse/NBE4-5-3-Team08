package com.team8.project2.domain.image.service

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.ByteArrayInputStream

@ExtendWith(MockitoExtension::class)
internal class S3UploaderTest {

    @Mock
    private lateinit var s3Client: S3Client

    @Mock
    private lateinit var multipartFile: MultipartFile

    private lateinit var s3Uploader: S3Uploader

    @BeforeEach
    fun setUp() {
        s3Uploader = S3Uploader(s3Client, "test-bucket")
    }

    @Test
    @DisplayName("S3 버킷에서 이미지를 삭제할 수 있다")
    fun deleteFile() {
        val fileName = "test-image.jpg"

        s3Uploader.deleteFile(fileName)

        verify(s3Client).deleteObject(any(DeleteObjectRequest::class.java))
    }

    @Test
    @DisplayName("S3 버킷에 이미지를 업로드할 수 있다")
    fun uploadFile() {
        `when`(multipartFile.originalFilename).thenReturn("test-image.jpg")
        `when`(multipartFile.contentType).thenReturn("image/jpeg")
        `when`(multipartFile.inputStream).thenReturn(ByteArrayInputStream(ByteArray(0)))
        `when`(multipartFile.size).thenReturn(10L)

        val uploadedFileName = s3Uploader.uploadFile(multipartFile)

        assertNotNull(uploadedFileName)
        verify(s3Client).putObject(any(PutObjectRequest::class.java), any(RequestBody::class.java))
    }
}
