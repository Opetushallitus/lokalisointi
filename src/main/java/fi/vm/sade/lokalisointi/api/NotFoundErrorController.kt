package fi.vm.sade.lokalisointi.api

import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.WebRequest

@RestController
class NotFoundErrorController : ErrorController {
    @RequestMapping(value = [ERROR_PATH])
    fun handleError(request: WebRequest?): Map<String, Any> {
        return DefaultErrorAttributes()
            .getErrorAttributes(request, ErrorAttributeOptions.defaults())
    }

    companion object {
        private const val ERROR_PATH = "/error"
    }
}
