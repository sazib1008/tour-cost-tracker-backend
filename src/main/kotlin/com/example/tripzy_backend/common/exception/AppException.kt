package com.example.tripzy_backend.common.exception

import org.springframework.http.HttpStatus

open class AppException(
    override val message: String,
    val status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    override val cause: Throwable? = null
) : RuntimeException(message, cause)

class ResourceNotFoundException(
    message: String
) : AppException(message, HttpStatus.NOT_FOUND)

class ConflictException(
    message: String
) : AppException(message, HttpStatus.CONFLICT)

class TourArchivedException(
    tourId: String = "",
    message: String = if (tourId.isNotBlank()) "Tour '$tourId' is archived and read-only. No modifications are allowed." else "Tour is archived and read-only. No modifications are allowed."
) : AppException(message, HttpStatus.CONFLICT)

class InvalidExpenseException(
    message: String
) : AppException(message, HttpStatus.BAD_REQUEST)

class UnauthorizedException(
    message: String = "Authentication is required to access this resource"
) : AppException(message, HttpStatus.UNAUTHORIZED)

class ForbiddenException(
    message: String = "You do not have permission to perform this action"
) : AppException(message, HttpStatus.FORBIDDEN)
