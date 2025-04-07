package com.team8.project2.domain.member.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Member {
    @Id // PRIMARY KEY
    @GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT
    @Setter(AccessLevel.PRIVATE)
    private Long id; // long -> null X, Long -> null O

    @CreatedDate
    @Setter(AccessLevel.PRIVATE)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Setter(AccessLevel.PRIVATE)
    private LocalDateTime modifiedDate;


    @Column(length = 100, unique = true)
    private String memberId;
    @Column(length = 100, unique = true, nullable = true)
    private String username;
    @Column(nullable = false)
    private String password;
    @Enumerated( EnumType.STRING)
    @Column(nullable = false)
    //@Builder.Default
    private RoleEnum role = RoleEnum.MEMBER;
    @Column
    private String profileImage;
    @Column
    private String email;
    @Column
    private String introduce;

    public Member(String memberId, String password, RoleEnum roleEnum, String email, String profileImage, String introduce) {
    }

    public Member(long id, String memberId) {
    }

    public Member(String memberId, String username, String password, RoleEnum roleEnum, String profileImage, String email, String introduce) {
    }

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
}
