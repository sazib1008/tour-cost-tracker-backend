package com.example.tripzy_backend.common.exception

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

class GlobalExceptionHandlerTest {

    private lateinit var mockMvc: MockMvc

    @RestController
    class TestExceptionController {
        @GetMapping("/test/not-found")
        fun notFound() {
            throw ResourceNotFoundException("Tour not found with id 123")
        }

        @GetMapping("/test/conflict")
        fun conflict() {
            throw ConflictException("Member already exists in tour")
        }

        @GetMapping("/test/archived")
        fun archived() {
            throw TourArchivedException("tour-abc")
        }

        @GetMapping("/test/invalid-expense")
        fun invalidExpense() {
            throw InvalidExpenseException("Payments sum does not equal expense amount")
        }

        @GetMapping("/test/illegal-argument")
        fun illegalArgument() {
            throw IllegalArgumentException("Invalid currency code provided")
        }

        @GetMapping("/test/unhandled")
        fun unhandled() {
            throw RuntimeException("Unexpected database crash")
        }
    }

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(TestExceptionController())
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    fun `should return 404 for ResourceNotFoundException`() {
        mockMvc.perform(get("/test/not-found").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Tour not found with id 123"))
            .andExpect(jsonPath("$.path").value("/test/not-found"))
    }

    @Test
    fun `should return 409 for ConflictException`() {
        mockMvc.perform(get("/test/conflict").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value("Member already exists in tour"))
    }

    @Test
    fun `should return 409 for TourArchivedException`() {
        mockMvc.perform(get("/test/archived").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value("Tour 'tour-abc' is archived and read-only. No modifications are allowed."))
    }

    @Test
    fun `should return 400 for InvalidExpenseException`() {
        mockMvc.perform(get("/test/invalid-expense").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Payments sum does not equal expense amount"))
    }

    @Test
    fun `should return 400 for IllegalArgumentException`() {
        mockMvc.perform(get("/test/illegal-argument").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Invalid currency code provided"))
    }

    @Test
    fun `should return 500 for generic unhandled Exception`() {
        mockMvc.perform(get("/test/unhandled").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."))
    }
}
