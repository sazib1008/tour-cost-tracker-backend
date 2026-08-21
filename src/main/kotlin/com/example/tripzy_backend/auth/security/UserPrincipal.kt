package com.example.tripzy_backend.auth.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.security.Principal

data class UserPrincipal(
    val id: String,
    val email: String,
    val fullName: String,
    val authoritiesList: Collection<GrantedAuthority> = listOf(SimpleGrantedAuthority("ROLE_USER"))
) : UserDetails, Principal {

    override fun getName(): String = id

    override fun getAuthorities(): Collection<GrantedAuthority> = authoritiesList

    override fun getPassword(): String = ""

    override fun getUsername(): String = email

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true
}
