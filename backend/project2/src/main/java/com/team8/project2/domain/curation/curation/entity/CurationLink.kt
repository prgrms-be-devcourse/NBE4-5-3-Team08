package com.team8.project2.domain.curation.curation.entity

import com.team8.project2.domain.link.entity.Link
import jakarta.persistence.*
import lombok.EqualsAndHashCode
import lombok.Getter
import lombok.Setter
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
    var id: CurationLinkId? = null

    /**
     * 큐레이션 엔티티와 다대일(N:1) 관계
     */
    @ManyToOne
    @JoinColumn(name = "curationId", insertable = false, updatable = false)
    var curation: Curation? = null

    /**
     * 링크 엔티티와 다대일(N:1) 관계
     */
    @ManyToOne
    @JoinColumn(name = "linkId", insertable = false, updatable = false)
    var link: Link? = null

    /**
     * 큐레이션과 링크의 복합 키를 정의하는 내부 클래스
     */
    @EqualsAndHashCode
    class CurationLinkId : Serializable {
        /** 큐레이션 ID  */
        @Column(name = "curationId")
        var curationId: Long? = null

        /** 링크 ID  */
        @Column(name = "linkId")
        var linkId: Long? = null
    }

    /**
     * 큐레이션과 링크를 설정하는 메서드
     * @param curation 큐레이션 엔티티
     * @param link 링크 엔티티
     * @return 설정된 CurationLink 객체
     */
    fun setCurationAndLink(curation: Curation, link: Link): CurationLink {
        val curationLinkId = CurationLinkId()
        curationLinkId.curationId = curation.id
        curationLinkId.linkId = link.id
        this.id = curationLinkId
        this.curation = curation
        this.link = link
        return this
    }
}

