package com.team8.project2.domain.link.entity

import com.team8.project2.domain.curation.curation.entity.CurationLink
import jakarta.persistence.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.io.IOException
import java.time.LocalDateTime

/**
 * 링크(Link) 엔티티 클래스입니다.
 * 큐레이션과 연관된 외부 링크 정보를 저장합니다.
 */
@Entity
@Table(name = "Link")
@EntityListeners(AuditingEntityListener::class)
class Link(

    /**
     * 링크의 고유 ID (자동 생성)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "linkId", nullable = false)
    var id: Long? = null,

    /**
     * 링크 URL (필수값)
     */
    @Column(name = "url", nullable = false)
    var url: String? = null,

    /**
     * 링크 클릭 수 (기본값 0)
     */
    @Column(name = "click", nullable = false)
    var click: Int = 0,

    /**
     * 링크 생성 시간 (자동 설정)
     */
    @CreatedDate
    @Column(name = "createdAt", nullable = false)
    var createdAt: LocalDateTime? = null,

    /**
     * 링크와 연관된 큐레이션 목록 (1:N 관계)
     */
    @OneToMany(mappedBy = "link", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var curationLinks: MutableList<CurationLink> = mutableListOf(),

    /**
     * 링크 제목
     */
    @Column(name = "title")
    var title: String? = null,

    /**
     * 링크 설명
     */
    @Column(name = "description")
    var description: String? = null,

    /**
     * 메타 이미지 URL
     */
    @Column(name = "metaImageUrl")
    var metaImageUrl: String? = null

) {

    fun loadMetadata() {
        try {
            val doc = Jsoup.connect(url ?: return).get()
            title = getMetaTagContent(doc, "og:title")
            description = getMetaTagContent(doc, "og:description")
            metaImageUrl = getMetaTagContent(doc, "og:image")
        } catch (e: IOException) {
            title = url
            description = url
        }
    }

    private fun getMetaTagContent(doc: Document, property: String): String {
        val metaTag: Element? = doc.select("meta[property=$property]").first()
        return metaTag?.attr("content") ?: ""
    }

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private val link = Link()

        fun id(id: Long) = apply { link.id = id }
        fun url(url: String) = apply { link.url = url }
        fun title(title: String) = apply { link.title = title }
        fun description(description: String) = apply { link.description = description }
        fun click(click: Int) = apply { link.click = click }
        fun build(): Link = link
    }
}
