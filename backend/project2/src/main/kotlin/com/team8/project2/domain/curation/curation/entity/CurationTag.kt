package com.team8.project2.domain.curation.curation.entity

import com.team8.project2.domain.curation.tag.entity.Tag
import jakarta.persistence.*
import java.io.Serializable

/**
 * 큐레이션과 태그 간의 관계를 나타내는 엔티티 클래스입니다.
 * 큐레이션과 태그는 다대다(N:M) 관계이며, 이를 매핑하기 위해 사용됩니다.
 */
@Entity
class CurationTag {

    /**
     * 복합 키를 정의하는 ID 클래스
     */
    @EmbeddedId
    lateinit var id: CurationTagId

    /**
     * 큐레이션 엔티티와 다대일(N:1) 관계
     */
    @ManyToOne(cascade = [CascadeType.PERSIST])
    @JoinColumn(name = "curationId", insertable = false, updatable = false)
    lateinit var curation: Curation

    /**
     * 태그 엔티티와 다대일(N:1) 관계
     */
    @ManyToOne(cascade = [CascadeType.PERSIST])
    @JoinColumn(name = "tagId", insertable = false, updatable = false)
    lateinit var tag: Tag

    /**
     * 큐레이션과 태그를 설정하는 메서드
     */
    fun setCurationAndTag(curation: Curation, tag: Tag): CurationTag {
        val curationTagId = CurationTagId(
            curationId = curation.id,
            tagId = tag.id
        )
        this.id = curationTagId
        this.curation = curation
        this.tag = tag
        return this
    }

    /**
     * 큐레이션과 태그의 복합 키를 정의하는 데이터 클래스
     */
    @Embeddable
    data class CurationTagId(
        @Column(name = "curationId")
        var curationId: Long? = null,

        @Column(name = "tagId")
        var tagId: Long? = null
    ) : Serializable
}
