package com.example.tripzy_backend.common.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.Instant

/**
 * Base class for MongoDB entities supporting auditing timestamps.
 */
abstract class BaseEntity {

    @CreatedDate
    var createdAt: Instant = Instant.now()

    @LastModifiedDate
    var updatedAt: Instant = Instant.now()
}
