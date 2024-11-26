package fi.vm.sade.lokalisointi.configuration;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TrailingSlashFilter implements Filter {

  /*
   * Enables mapping trailing slash to a path without the trailing slash, such as
   * /api/v1/lokalisointi/ -> /api/v1/lokalisointi
   */
  @Override
  public void doFilter(
      final ServletRequest request, final ServletResponse response, final FilterChain chain)
      throws IOException, ServletException {
    final HttpServletRequestWrapper requestWrapper =
        new HttpServletRequestWrapper((HttpServletRequest) request) {
          @Override
          public String getRequestURI() {
            final String requestURI = super.getRequestURI();
            if (StringUtils.endsWith(requestURI, "/")) {
              return StringUtils.removeEnd(requestURI, "/");
            }
            return requestURI;
          }
        };
    chain.doFilter(requestWrapper, response);
  }
}
