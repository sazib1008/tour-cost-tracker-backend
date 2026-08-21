package com.example.tripzy_backend.user.model

import com.example.tripzy_backend.common.domain.BaseEntity
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "users")
data class User(
    @Id
    var id: String? = null,

    @Indexed(unique = true)
    var email: String,

    var name: String,

    var avatarUrl: String? = null,

    @Indexed(unique = true, sparse = true)
    var googleId: String? = null
) : BaseEntity()
