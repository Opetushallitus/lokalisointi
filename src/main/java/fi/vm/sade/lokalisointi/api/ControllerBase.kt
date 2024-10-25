package fi.vm.sade.lokalisointi.api

import org.springframework.data.relational.core.conversion.DbActionExecutionException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus

abstract class ControllerBase {
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(
        DbActionExecutionException::class, IllegalArgumentException::class, HttpMessageNotReadableException::class
    )
    fun handleUserErrors(ex: RuntimeException): Map<String, Map<String, String?>> {
        return mapOf(
            Pair(
                "error", listOf(
                    Pair("message", ex.message ?: ex.toString()),
                    Pair("cause", ex.cause?.message)
                ).filter { p -> p.second != null }.toMap()
            )
        )
    }
}
