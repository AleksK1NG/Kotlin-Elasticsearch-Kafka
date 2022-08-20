package com.search.microservice.controller

import com.search.microservice.exceptions.ErrorHttpResponse
import com.search.microservice.exceptions.SerializationException
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebInputException
import reactor.util.Loggers
import java.time.LocalDateTime


@Order(2)
@ControllerAdvice
class GlobalControllerAdvice {

    @ExceptionHandler(value = [RuntimeException::class])
    fun handleRuntimeException(ex: RuntimeException, request: ServerHttpRequest): ResponseEntity<ErrorHttpResponse> {
        val errorHttpResponse = ErrorHttpResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.message ?: "", LocalDateTime.now().toString())
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON).body(errorHttpResponse).also {
            log.error("(GlobalControllerAdvice) RuntimeException", ex)
        }
    }

    @ExceptionHandler(value = [IllegalArgumentException::class])
    fun handleIllegalArgumentException(ex: IllegalArgumentException, request: ServerHttpRequest): ResponseEntity<ErrorHttpResponse> {
        val errorHttpResponse = ErrorHttpResponse(HttpStatus.BAD_REQUEST.value(), ex.message ?: "", LocalDateTime.now().toString())
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(errorHttpResponse)
            .also { log.error("(GlobalControllerAdvice) IllegalArgumentException BAD_REQUEST", ex) }
    }

    @ExceptionHandler(value = [SerializationException::class, ServerWebInputException::class])
    fun handleBadRequestException(ex: RuntimeException, request: ServerHttpRequest): ResponseEntity<ErrorHttpResponse> {
        val errorHttpResponse = ErrorHttpResponse(HttpStatus.BAD_REQUEST.value(), ex.message ?: "", LocalDateTime.now().toString())
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(errorHttpResponse)
            .also { log.error("(GlobalControllerAdvice) BAD_REQUEST", ex) }
    }


    companion object {
        private val log = Loggers.getLogger(GlobalControllerAdvice::class.java)
    }
}