package com.example.tripzy_backend.tour.controller

import com.example.tripzy_backend.auth.security.UserPrincipal
import com.example.tripzy_backend.common.response.ApiResponse
import com.example.tripzy_backend.tour.dto.AddMemberRequest
import com.example.tripzy_backend.tour.dto.CreateTourRequest
import com.example.tripzy_backend.tour.dto.JoinTourRequest
import com.example.tripzy_backend.tour.dto.TourDetailDto
import com.example.tripzy_backend.tour.dto.TourSummaryDto
import com.example.tripzy_backend.tour.dto.UpdateTourRequest
import com.example.tripzy_backend.tour.dto.UpdateTourStatusRequest
import com.example.tripzy_backend.tour.service.TourService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tours")
class TourController(
    private val tourService: TourService
) {

    @PostMapping
    fun createTour(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: CreateTourRequest
    ): ResponseEntity<ApiResponse<TourDetailDto>> {
        val tour = tourService.createTour(principal.id, request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(data = tour, message = "Tour created successfully"))
    }

    @GetMapping
    fun getMyTours(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<List<TourSummaryDto>>> {
        val tours = tourService.getUserTours(principal.id)
        return ResponseEntity.ok(ApiResponse.success(tours))
    }

    @GetMapping("/{id}")
    fun getTourById(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String
    ): ResponseEntity<ApiResponse<TourDetailDto>> {
        val tour = tourService.getTourDetails(principal.id, id)
        return ResponseEntity.ok(ApiResponse.success(tour))
    }

    @PutMapping("/{id}")
    fun updateTour(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateTourRequest
    ): ResponseEntity<ApiResponse<TourDetailDto>> {
        val tour = tourService.updateTour(principal.id, id, request)
        return ResponseEntity.ok(ApiResponse.success(data = tour, message = "Tour updated successfully"))
    }

    @PostMapping("/join")
    fun joinTour(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: JoinTourRequest
    ): ResponseEntity<ApiResponse<TourDetailDto>> {
        val tour = tourService.joinTourByCode(principal.id, request)
        return ResponseEntity.ok(ApiResponse.success(data = tour, message = "Joined tour successfully"))
    }

    @PostMapping("/{id}/members")
    fun addMember(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String,
        @Valid @RequestBody request: AddMemberRequest
    ): ResponseEntity<ApiResponse<TourDetailDto>> {
        val tour = tourService.addMember(principal.id, id, request)
        return ResponseEntity.ok(ApiResponse.success(data = tour, message = "Member added successfully"))
    }

    @DeleteMapping("/{id}/members/{targetUserId}")
    fun removeMember(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String,
        @PathVariable targetUserId: String
    ): ResponseEntity<ApiResponse<Unit>> {
        tourService.removeMember(principal.id, id, targetUserId)
        val msg = if (principal.id == targetUserId) "Left tour successfully" else "Member removed successfully"
        return ResponseEntity.ok(ApiResponse.successMessage(msg))
    }

    @PatchMapping("/{id}/status")
    fun updateStatus(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateTourStatusRequest
    ): ResponseEntity<ApiResponse<TourDetailDto>> {
        val tour = tourService.updateTourStatus(principal.id, id, request)
        return ResponseEntity.ok(ApiResponse.success(data = tour, message = "Tour status updated successfully"))
    }
}
