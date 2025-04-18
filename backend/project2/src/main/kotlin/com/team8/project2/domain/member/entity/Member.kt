package com.team8.project2.domain.member.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime

@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "members")
class Member(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(length = 100, unique = true)
    private var loginId: String? = null,

    @Column(nullable = false)
    private var password: String? = null,

    @Column(length = 100, unique = true)
    private var username: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: RoleEnum = RoleEnum.MEMBER,

    @Column
    var profileImage: String? = null,

    @Column
    var email: String? = null,

    @Column
    var introduce: String? = null
) {

    @CreatedDate
    var createdDate: LocalDateTime? = null

    @LastModifiedDate
    var modifiedDate: LocalDateTime? = null

    init {
        // 예: username이 null이면 "anonymous" 할당
        if (username == null) {
            username = "anonymous"
        }
    }

    // getter 예시
    fun getLoginId(): String = loginId ?: throw UninitializedPropertyAccessException("loginId is not initialized")
    fun getUsername(): String = username ?: throw UninitializedPropertyAccessException("username is not initialized")
    fun getPassword(): String = password ?: throw UninitializedPropertyAccessException("password is not initialized")
    fun setUsername(username: String) {
        this.username = username
    }

    // 상태
    val isAdmin: Boolean get() = this.role == RoleEnum.ADMIN
    val isMember: Boolean get() = this.role == RoleEnum.MEMBER

    val authorities: Collection<GrantedAuthority>
        get() = memberAuthoritesAsString.map { SimpleGrantedAuthority(it) }

    val memberAuthoritesAsString: List<String>
        get() = buildList {
            if (isAdmin) add("ROLE_ADMIN")
        }
}