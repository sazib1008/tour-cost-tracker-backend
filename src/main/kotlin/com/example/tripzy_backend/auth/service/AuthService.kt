package com.example.tripzy_backend.auth.service

import com.example.tripzy_backend.auth.dto.AuthResponse
import com.example.tripzy_backend.auth.dto.GoogleAuthRequest
import com.example.tripzy_backend.user.dto.UserDto
import com.example.tripzy_backend.user.service.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val googleTokenVerifierService: GoogleTokenVerifierService,
    private val userService: UserService,
    private val jwtService: JwtService
) {

    @Transactional
    fun authenticateWithGoogle(request: GoogleAuthRequest): AuthResponse {
        val payload = googleTokenVerifierService.verifyToken(request.idToken)
        val user = userService.findOrCreateGoogleUser(
            googleId = payload.sub,
            email = payload.email,
            name = payload.name,
            avatarUrl = payload.picture
        )

        val userId = user.id ?: throw IllegalStateException("User id must not be null after persisting")
        val jwtToken = jwtService.generateToken(
            userId = userId,
            email = user.email,
            name = user.name
        )

        return AuthResponse(
            token = jwtToken,
            user = UserDto.fromEntity(user),
            expiresInMs = jwtService.expirationMs
        )
    }
}
