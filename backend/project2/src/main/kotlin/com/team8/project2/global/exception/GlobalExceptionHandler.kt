package com.team8.project2.global.exception

import com.team8.project2.global.dto.RsData
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<RsData<Void>> {
        val message = e.bindingResult.fieldErrors
            .joinToString("\n") { "${it.field} : ${it.code} : ${it.defaultMessage}" }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(RsData("400-1", message))
    }

    @ResponseStatus
    @ExceptionHandler(ServiceException::class)
    fun handleServiceException(ex: ServiceException): ResponseEntity<RsData<Void>> {
        return ResponseEntity
            .status(ex.statusCode)
            .body(RsData(ex.code, ex.msg))
    }

    @ResponseStatus
    @ExceptionHandler(BadRequestException::class, IllegalArgumentException::class)
    fun handleBadRequestException(e: RuntimeException): ResponseEntity<RsData<Void>> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(RsData("400-2", e.message ?: "잘못된 요청입니다."))
    }

    @ResponseStatus
    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(e: NotFoundException): ResponseEntity<RsData<Void>> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(RsData("404-1", e.message ?: "요청한 리소스를 찾을 수 없습니다."))
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(ex: AccessDeniedException): ResponseEntity<RsData<Void>> {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(RsData("401-1", "접근이 거부되었습니다. 로그인 상태를 확인해 주세요."))
    }

    @ResponseStatus
    @ExceptionHandler(Exception::class)
    fun handleGlobalException(e: Exception): ResponseEntity<RsData<Void>> {
        logger.error("Unhandled exception occurred", e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(RsData("500-1", "서버 내부 오류가 발생했습니다. 관리자에게 문의하세요."))
    }
}
