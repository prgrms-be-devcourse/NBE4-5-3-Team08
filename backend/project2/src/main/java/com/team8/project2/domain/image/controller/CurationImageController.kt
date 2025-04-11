package com.team8.project2.domain.image.controller

import com.team8.project2.domain.image.service.CurationImageService
import com.team8.project2.global.exception.ServiceException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.IOException

@RestController
@RequestMapping("/api/v1/images")
class CurationImageController(
    private val curationImageService: CurationImageService
) {

    @PostMapping("/upload")
    fun uploadCurationImage(@RequestParam("file") file: MultipartFile): ResponseEntity<String> {
        val imageUrl = try {
            curationImageService.uploadImage(file)
        } catch (e: IOException) {
            throw ServiceException("500-1", "이미지 업로드에 실패했습니다.")
        }

        return ResponseEntity.ok(imageUrl)
    }
}
