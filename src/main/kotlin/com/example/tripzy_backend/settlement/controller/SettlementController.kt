package com.example.tripzy_backend.settlement.controller

import com.example.tripzy_backend.auth.security.UserPrincipal
import com.example.tripzy_backend.common.response.ApiResponse
import com.example.tripzy_backend.settlement.dto.RecordSettlementRequest
import com.example.tripzy_backend.settlement.dto.SettlementDto
import com.example.tripzy_backend.settlement.service.SettlementService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tours/{tourId}/settlements")
class SettlementController(
    private val settlementService: SettlementService
) {

    @PostMapping
    fun recordSettlement(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable tourId: String,
        @Valid @RequestBody request: RecordSettlementRequest
    ): ResponseEntity<ApiResponse<SettlementDto>> {
        val settlement = settlementService.recordSettlement(principal.id, tourId, request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(data = settlement, message = "Settlement recorded successfully"))
    }

    @GetMapping
    fun getTourSettlements(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable tourId: String
    ): ResponseEntity<ApiResponse<List<SettlementDto>>> {
        val settlements = settlementService.getTourSettlements(principal.id, tourId)
        return ResponseEntity.ok(ApiResponse.success(settlements))
    }
}
