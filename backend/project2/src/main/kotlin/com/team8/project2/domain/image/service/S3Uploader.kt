package com.team8.project2.domain.image.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.IOException
import java.util.UUID

@Service
class S3Uploader(
    private val s3Client: S3Client,
    @Value("\${spring.cloud.aws.s3.bucket}")
    private val bucketName: String
) {
    fun deleteFile(imageName: String) {
        val deleteObjectRequest = DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(imageName)
            .build()

        s3Client.deleteObject(deleteObjectRequest)
    }

    @Throws(IOException::class)
    fun uploadFile(file: MultipartFile): String {
        val fileName = generateUniqueFileName(file.originalFilename)

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(file.contentType)
                .build(),
            RequestBody.fromInputStream(file.inputStream, file.size)
        )

        return fileName
    }

    val baseUrl: String
        get() = "https://$bucketName.s3.amazonaws.com/"

    private fun generateUniqueFileName(originalName: String?): String =
        "${UUID.randomUUID()}_${originalName ?: "unknown"}"
}