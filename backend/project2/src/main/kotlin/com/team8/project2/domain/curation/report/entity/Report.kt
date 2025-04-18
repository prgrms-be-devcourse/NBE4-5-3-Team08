package com.team8.project2.domain.curation.report.entity

import com.team8.project2.domain.curation.curation.entity.Curation
import com.team8.project2.domain.member.entity.Member
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "Report")
@EntityListeners(AuditingEntityListener::class)
class Report(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reportId")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curationId")
    val curation: Curation,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporterId")
    val reporter: Member,

    @Enumerated(EnumType.STRING)
    val reportType: ReportType,

    @CreatedDate
    @Column(updatable = false)
    var reportDate: LocalDateTime? = null
)
