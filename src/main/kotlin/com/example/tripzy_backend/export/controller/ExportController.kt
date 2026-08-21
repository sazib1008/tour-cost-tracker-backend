package com.example.tripzy_backend.export.controller

import com.example.tripzy_backend.auth.security.UserPrincipal
import com.example.tripzy_backend.export.service.ExportService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tours/{tourId}/export")
class ExportController(
    private val exportService: ExportService
) {

    @GetMapping
    fun exportTour(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable tourId: String,
        @RequestParam(defaultValue = "pdf") format: String
    ): ResponseEntity<ByteArray> {
        val result = exportService.exportTour(principal.id, tourId, format)

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${result.fileName}\"")
            .contentType(MediaType.parseMediaType(result.contentType))
            .body(result.data)
    }
}
