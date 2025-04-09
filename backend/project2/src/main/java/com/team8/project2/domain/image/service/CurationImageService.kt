package com.team8.project2.domain.image.service

import com.team8.project2.domain.image.entity.CurationImage
import com.team8.project2.domain.image.repository.CurationImageRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.IOException

@Service
class CurationImageService(
    private val s3Uploader: S3Uploader,
    private val curationImageRepository: CurationImageRepository
) {

    @Transactional
    @Throws(IOException::class)
    fun uploadImage(file: MultipartFile): String {
        val imageName = s3Uploader.uploadFile(file)

        val curationImage = CurationImage(imageName)
        curationImageRepository.save(curationImage)

        return s3Uploader.baseUrl + imageName
    }

    @Transactional(readOnly = true)
    fun findByCurationId(curationId: Long): List<CurationImage> {
        return curationImageRepository.findByCurationId(curationId)
    }

    @Transactional
    fun deleteByImageName(imageName: String) {
        curationImageRepository.deleteByImageName(imageName)
    }

    @Transactional
    fun deleteByCurationId(curationId: Long) {
        curationImageRepository.deleteByCurationId(curationId)
    }
}
