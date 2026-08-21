package com.example.tripzy_backend.balance.controller

import com.example.tripzy_backend.auth.security.UserPrincipal
import com.example.tripzy_backend.balance.dto.TourBalanceResponseDto
import com.example.tripzy_backend.balance.service.BalanceService
import com.example.tripzy_backend.common.response.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tours/{tourId}/balance")
class BalanceController(
    private val balanceService: BalanceService
) {

    @GetMapping
    fun getTourBalance(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable tourId: String
    ): ResponseEntity<ApiResponse<TourBalanceResponseDto>> {
        val balance = balanceService.getTourBalance(principal.id, tourId)
        return ResponseEntity.ok(ApiResponse.success(balance))
    }
}
