package com.team8.project2.domain.member.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Member {
    @Id // PRIMARY KEY
    @GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT
    @Setter(AccessLevel.PRIVATE)
    public Long id; // long -> null X, Long -> null O // 임시 public

    @CreatedDate
    @Setter(AccessLevel.PRIVATE)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Setter(AccessLevel.PRIVATE)
    private LocalDateTime modifiedDate;


    @Column(length = 100, unique = true)
    public String memberId; // Kotlin 전환으로 임시 public 설정
    @Column(length = 100, unique = true, nullable = true)
    private String username;
    @Column(nullable = false)
    private String password;
    @Enumerated( EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RoleEnum role = RoleEnum.MEMBER;
    @Column
    private String profileImage;
    @Column
    private String email;
    @Column
    private String introduce;

    public boolean isAdmin() {return this.role == RoleEnum.ADMIN;}
    public boolean isMember() {
        return this.role == RoleEnum.MEMBER;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {

        return getMemberAuthoritesAsString()
                .stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

    }

    public List<String> getMemberAuthoritesAsString() {

        List<String> authorities = new ArrayList<>();

        if(isAdmin()) {
            authorities.add("ROLE_ADMIN");
        }

        return authorities;
    }

    // TODO : ✅ Kotlin에서 접근 가능하도록 명시적 getter 추가
    public Long getId() {
        return this.id;
    }

    // TODO : ✅ Kotlin에서 접근 가능하도록 명시적 getter 추가
    public String getUsername() {
        return this.username;
    }

    // TODO : ✅ Kotlin에서 접근 가능하도록 명시적 getter 추가
    public String getProfileImage() {
        return this.profileImage;
    }

    public Member(String email, RoleEnum role, String memberId, String username, String password, String profileImage, String introduce) {
        this.email = email;
        this.role = role;
        this.memberId = memberId;
        this.username = username;
        this.password = password;
        this.profileImage = profileImage;
        this.introduce = introduce;
    }
  
    // TODO : ✅ Kotlin에서 접근 가능하도록 명시적 getter 추가
    public String getMemberId() {
        return this.memberId;
    }

    // TODO : ✅ Kotlin에서 접근 가능하도록 명시적 getter 추가
    public String getPassword() {
        return this.password;
    }
}
