package com.team8.project2.domain.image.repository

import com.team8.project2.domain.image.entity.CurationImage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface CurationImageRepository : JpaRepository<CurationImage, Long> {
    fun findByImageName(imageUrl: String): CurationImage?

    fun findByCurationId(curationId: Long): List<CurationImage>

    fun deleteByImageName(imageName: String)

    fun deleteByCurationId(curationId: Long)

    @Query("SELECT c FROM CurationImage c WHERE c.curationId IS NULL AND c.uploadedAt <= :cutoffDate")
    fun findUnusedImages(cutoffDate: LocalDateTime): List<CurationImage>
}
