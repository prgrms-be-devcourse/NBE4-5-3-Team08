package com.team8.project2.domain.curation.curation.entity

import com.team8.project2.domain.comment.entity.Comment
import com.team8.project2.domain.member.entity.Member
import jakarta.persistence.*
import org.jsoup.Jsoup
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * 큐레이션(Curation) 엔티티 클래스입니다.
 * 큐레이션은 사용자(Member)가 생성한 컨텐츠로, 제목, 내용, 태그, 링크 등을 포함할 수 있습니다.
 */
@Entity
@EntityListeners(
    AuditingEntityListener::class
)
@Table(name = "Curation")
class Curation(
    /**
     * 큐레이션 제목 (필수값)
     */
    @Column(name = "title", nullable = false)
    var title: String,
    /**
     * 큐레이션 내용 (필수값, TEXT 타입 지정)
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String,
    /**
     * 큐레이션 작성자 (Member와 N:1 관계, 선택적)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memberId", nullable = true)
    val member: Member,
) {
    /**
     * 큐레이션의 고유 ID (자동 생성)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "curationId", nullable = false)
    var id: Long? = null

    /**
     * 큐레이션 생성 시간 (자동 설정)
     */
    @CreatedDate
    @Column(name = "createdAt", nullable = false)
    var createdAt: LocalDateTime? = null

    /**
     * 큐레이션 마지막 수정 시간 (자동 설정)
     */
    @LastModifiedDate
    @Column(name = "modifiedAt")
    var modifiedAt: LocalDateTime? = null

    /**
     * 큐레이션 좋아요 수 (기본값 0)
     */
    @Column(name = "likeCount", nullable = false)
    var likeCount = 0L


    /**
     * 큐레이션 조회 수 (기본값 0)
     */
    @Column(name = "viewCount", nullable = false)
    var viewCount = 0L

    /**
     * 큐레이션에 포함된 링크 목록 (CurationLink와 1:N 관계)
     */
    @OneToMany(mappedBy = "curation", fetch = FetchType.LAZY, orphanRemoval = true)
    var curationLinks: MutableList<CurationLink> = mutableListOf()

    /**
     * 큐레이션에 포함된 태그 목록 (CurationTag와 1:N 관계)
     */
    @OneToMany(mappedBy = "curation", fetch = FetchType.LAZY, orphanRemoval = true)
    var tags: MutableList<CurationTag> = mutableListOf()

    /**
     * 큐레이션에 포함된 댓글 목록 (Comment와 1:N 관계)
     */
    @OneToMany(mappedBy = "curation", fetch = FetchType.LAZY, orphanRemoval = true)
    var comments: MutableList<Comment> = mutableListOf()

    /**
     * 큐레이션 좋아요 수 증가 메서드
     */
    fun like() {
        likeCount++
    }

    // 조회수 증가 메서드
    fun increaseViewCount() {
        viewCount++
    }


    val memberName: String
        get() = member.getUsername()

    val memberId: Long
        get() = member.id!!

    val memberImgUrl: String?
        get() = member.profileImage

    val imageNames: List<String>
        get() {
            val imageFileNames: MutableList<String> = ArrayList()
            val document = Jsoup.parse(content)
            val images = document.select("img[src]")

            for (img in images) {
                val src = img.attr("src")
                if (src.startsWith("https://linkurator-bucket")) {
                    val fileName = extractFileNameFromUrl(src)
                    imageFileNames.add(fileName)
                }
            }
            return imageFileNames
        }

    private fun extractFileNameFromUrl(fileUrl: String): String {
        return fileUrl.substring(fileUrl.lastIndexOf("/") + 1)
    }

    val commentCount: Int
        get() = comments.size



}