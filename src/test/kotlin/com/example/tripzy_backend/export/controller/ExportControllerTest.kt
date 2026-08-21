package com.example.tripzy_backend.export.controller

import com.example.tripzy_backend.auth.security.UserPrincipal
import com.example.tripzy_backend.common.exception.GlobalExceptionHandler
import com.example.tripzy_backend.export.service.ExportResult
import com.example.tripzy_backend.export.service.ExportService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

class ExportControllerTest {

    private val exportService: ExportService = mockk()
    private lateinit var mockMvc: MockMvc

    private val mockPrincipal = UserPrincipal(
        id = "user-123",
        email = "user@example.com",
        fullName = "Test User"
    )

    private val principalResolver = object : HandlerMethodArgumentResolver {
        override fun supportsParameter(parameter: MethodParameter): Boolean {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal::class.java)
        }

        override fun resolveArgument(
            parameter: MethodParameter,
            mavContainer: ModelAndViewContainer?,
            webRequest: NativeWebRequest,
            binderFactory: WebDataBinderFactory?
        ): Any {
            return mockPrincipal
        }
    }

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ExportController(exportService))
            .setCustomArgumentResolvers(principalResolver)
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    fun `GET export csv should return csv attachment`() {
        val result = ExportResult(
            fileName = "trip_expenses.csv",
            contentType = "text/csv",
            data = "Date,Title,Amount\n2026-08-20,Lunch,5000\n".toByteArray()
        )

        every { exportService.exportTour("user-123", "t1", "csv") } returns result

        mockMvc.perform(get("/tours/t1/export?format=csv"))
            .andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"trip_expenses.csv\""))
            .andExpect(content().contentType("text/csv"))
    }

    @Test
    fun `GET export pdf should return pdf attachment`() {
        val result = ExportResult(
            fileName = "trip_report.pdf",
            contentType = "application/pdf",
            data = "%PDF-1.4 test bytes".toByteArray()
        )

        every { exportService.exportTour("user-123", "t1", "pdf") } returns result

        mockMvc.perform(get("/tours/t1/export?format=pdf"))
            .andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"trip_report.pdf\""))
            .andExpect(content().contentType("application/pdf"))
    }
}
