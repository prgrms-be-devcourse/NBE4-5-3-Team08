package com.team8.project2.domain.curation.curation.entity

import com.team8.project2.domain.link.entity.Link
import jakarta.persistence.*
import java.io.Serializable

/**
 * 큐레이션과 링크 간의 관계를 나타내는 엔티티 클래스입니다.
 * 큐레이션과 링크는 다대다(N:M) 관계이며, 이를 매핑하기 위해 사용됩니다.
 */
@Entity
class CurationLink {

    /**
     * 복합 키를 정의하는 ID 클래스
     */
    @EmbeddedId
    lateinit var id: CurationLinkId

    /**
     * 큐레이션 엔티티와 다대일(N:1) 관계
     */
    @ManyToOne
    @JoinColumn(name = "curationId", insertable = false, updatable = false)
    lateinit var curation: Curation

    /**
     * 링크 엔티티와 다대일(N:1) 관계
     */
    @ManyToOne
    @JoinColumn(name = "linkId", insertable = false, updatable = false)
    lateinit var link: Link

    /**
     * 큐레이션과 링크를 설정하는 메서드
     */
    fun setCurationAndLink(curation: Curation, link: Link): CurationLink {
        val curationLinkId = CurationLinkId(
            curationId = curation.id,
            linkId = link.id
        )
        this.id = curationLinkId
        this.curation = curation
        this.link = link
        return this
    }

    /**
     * 큐레이션과 링크의 복합 키를 정의하는 데이터 클래스
     */
    @Embeddable
    data class CurationLinkId(
        @Column(name = "curationId")
        var curationId: Long? = null,

        @Column(name = "linkId")
        var linkId: Long? = null
    ) : Serializable
}
