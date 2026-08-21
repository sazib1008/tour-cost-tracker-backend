package com.example.tripzy_backend.common.domain

/**
 * Tour lifecycle state.
 *
 * ACTIVE: Expenses can be added, edited, splits calculated, members invited.
 * SETTLED: All net balances are zero. Tour is settled but not yet archived.
 * ARCHIVED: Tour is closed and strictly read-only. All write/mutation attempts must return 409 Conflict.
 */
enum class TourStatus {
    ACTIVE,
    SETTLED,
    ARCHIVED;

    fun isArchived(): Boolean = this == ARCHIVED
}
