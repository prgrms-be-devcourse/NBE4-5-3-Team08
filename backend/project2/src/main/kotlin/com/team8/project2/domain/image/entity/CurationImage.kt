package com.team8.project2.domain.image.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "curationImages")
@EntityListeners(AuditingEntityListener::class)
class CurationImage(
    @Column(name = "imageName", nullable = false, unique = true, updatable = false)
    val imageName: String,
) {
    @Column(name = "curationId")
    var curationId: Long? = null

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "curationImageId", nullable = false)
    val id: Long? = null

    @CreatedDate
    @Column(name = "uploadedAt", nullable = false, updatable = false)
    val uploadedAt: LocalDateTime? = null

    fun setCurationIdIfNull(curationId: Long) {
        this.curationId = this.curationId ?: curationId
    }
}
