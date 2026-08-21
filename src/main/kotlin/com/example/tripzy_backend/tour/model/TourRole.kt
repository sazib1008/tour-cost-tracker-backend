package com.example.tripzy_backend.tour.model

enum class TourRole {
    ADMIN,
    MEMBER;

    fun isAdmin(): Boolean = this == ADMIN
}
