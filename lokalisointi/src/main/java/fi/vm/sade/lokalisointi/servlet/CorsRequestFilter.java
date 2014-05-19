package fi.vm.sade.lokalisointi.servlet;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

/**
 * NOTE: Lokalisointi CORS handling is bit different since we want to copy lokalisations from one environment to another.
 *
 * @see AllowAllCorsRequestsInDevModeServletFilter for generic implementation
 * @author mlyly
 */
public class CorsRequestFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(CorsRequestFilter.class);

    @Value("${auth.mode}")
    private String authMode;

    private boolean isDev = false;

    @Value("${cors.allow-origin.lokalisointi}")
    private String allowOrigin;

    private String[] allowOrigins = {};

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this,
                filterConfig.getServletContext());
        isDev = "dev".equals(authMode);
        LOG.info("Cors filter startied in (dev mode=) " + isDev + " mode. Env 'cors.allow-origin.lokalisointi' == " + allowOrigin);
        
        if (allowOrigin != null) {
            allowOrigins = allowOrigin.split(",");
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        LOG.debug("doFilter()");

        if (isDev) {
            if (response instanceof HttpServletResponse) {
                HttpServletRequest req = (HttpServletRequest) request;
                String headerOrigin = req.getHeader("Origin");

                if (headerOrigin != null) {
                    LOG.debug("  fixing CORS --> allow: '{}'", headerOrigin);

                    HttpServletResponse res = (HttpServletResponse) response;
                    res.addHeader("Access-Control-Allow-Origin", headerOrigin);
                    res.addHeader("Access-Control-Allow-Credentials", "true");
                    res.addHeader("Access-Control-Allow-Methods",
                            "POST, GET, OPTIONS, PUT, DELETE, HEAD");
                    res.addHeader("Access-Control-Allow-Headers",
                            "X-PINGOTHER, Origin, X-Requested-With, Content-Type, Accept");
                    res.addHeader("Access-Control-Max-Age", "1728000");
                }
            }
        } else {
            if (response instanceof HttpServletResponse) {
                HttpServletRequest req = (HttpServletRequest) request;
                String headerOrigin = req.getHeader("Origin");

                if (allowOrigin != null) {
                    // Does the url match list of allowed?
                    boolean originMatch = false;
                    for (String origin : allowOrigins) {
                        if (headerOrigin != null) {
                            originMatch = originMatch || headerOrigin.contains(origin);
                            LOG.info("  checking: {} against - {}", headerOrigin, origin);
                        }
                    }

                    if (originMatch) {
                        LOG.info("  origin match! fixing PRODUCTION CORS --> allow: '{}'", allowOrigin);

                        HttpServletResponse res = (HttpServletResponse) response;
                        res.addHeader("Access-Control-Allow-Origin", headerOrigin);
                        res.addHeader("Access-Control-Allow-Credentials", "true");
                        res.addHeader("Access-Control-Allow-Methods",
                                "POST, GET, OPTIONS, PUT, DELETE, HEAD");
                        res.addHeader("Access-Control-Allow-Headers",
                                "X-PINGOTHER, Origin, X-Requested-With, Content-Type, Accept");
                        res.addHeader("Access-Control-Max-Age", "1728000");
                    } else {
                        LOG.warn("Original uri={} is not in allowed uris list... sorry.", headerOrigin);
                    }
                }
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        LOG.info("destroy()");
    }

}
