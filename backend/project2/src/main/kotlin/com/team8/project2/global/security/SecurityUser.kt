package com.team8.project2.global.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User

class SecurityUser(
    val id: Long,
    username: String,
    password: String,
    authorities: Collection<GrantedAuthority?>
) : User(username, password, authorities)
