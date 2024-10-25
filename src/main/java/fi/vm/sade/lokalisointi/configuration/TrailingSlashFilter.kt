package fi.vm.sade.lokalisointi.configuration

import jakarta.servlet.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class TrailingSlashFilter : Filter {
    /*
      * Enables mapping trailing slash to a path without the trailing slash, such as
      * /api/v1/lokalisointi/ -> /api/v1/lokalisointi
      */
    @Throws(IOException::class, ServletException::class)
    override fun doFilter(
        request: ServletRequest, response: ServletResponse, chain: FilterChain
    ) {
        val requestWrapper: HttpServletRequestWrapper =
            object : HttpServletRequestWrapper(request as HttpServletRequest) {
                override fun getRequestURI(): String {
                    val requestURI = super.getRequestURI()
                    if (StringUtils.endsWith(requestURI, "/")) {
                        return StringUtils.removeEnd(requestURI, "/")
                    }
                    return requestURI
                }
            }
        chain.doFilter(requestWrapper, response)
    }
}
