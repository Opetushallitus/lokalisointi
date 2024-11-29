package fi.vm.sade.lokalisointi.configuration;

import fi.vm.sade.javautils.kayttooikeusclient.OphUserDetailsServiceImpl;
import org.apereo.cas.client.session.SingleSignOutFilter;
import org.apereo.cas.client.validation.Cas20ProxyTicketValidator;
import org.apereo.cas.client.validation.TicketValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.authentication.CasAuthenticationProvider;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.*;
import java.util.stream.Stream;

@Profile({"default", "dev"})
@Configuration
@EnableMethodSecurity(securedEnabled = true)
@EnableWebSecurity
public class SecurityConfiguration implements InitializingBean {
  private static final Logger LOG = LoggerFactory.getLogger(SecurityConfiguration.class);
  private final CasProperties casProperties;
  private final Environment environment;

  @Value("${lokalisointi.cors.allow-origin}")
  private String allowOrigin;

  private String[] allowOrigins = {};

  @Autowired
  public SecurityConfiguration(final CasProperties casProperties, final Environment environment) {
    this.casProperties = casProperties;
    this.environment = environment;
    LOG.info("CAS props: {}", casProperties);
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (allowOrigin != null) {
      allowOrigins = Arrays.stream(allowOrigin.split(",")).map(String::trim).toArray(String[]::new);
      LOG.info("Allow origins: {}", Arrays.toString(allowOrigins));
    }
  }

  @Bean
  public ServiceProperties serviceProperties() {
    final ServiceProperties serviceProperties = new ServiceProperties();
    serviceProperties.setService(casProperties.getService() + "/j_spring_cas_security_check");
    serviceProperties.setSendRenew(casProperties.getSendRenew());
    serviceProperties.setAuthenticateAllArtifacts(true);
    return serviceProperties;
  }

  @Bean
  public CasAuthenticationProvider casAuthenticationProvider() {
    final CasAuthenticationProvider casAuthenticationProvider = new CasAuthenticationProvider();
    casAuthenticationProvider.setAuthenticationUserDetailsService(new OphUserDetailsServiceImpl());
    casAuthenticationProvider.setServiceProperties(serviceProperties());
    casAuthenticationProvider.setTicketValidator(ticketValidator());
    casAuthenticationProvider.setKey(casProperties.getKey());
    return casAuthenticationProvider;
  }

  @Bean
  public TicketValidator ticketValidator() {
    final Cas20ProxyTicketValidator ticketValidator =
        new Cas20ProxyTicketValidator(environment.getRequiredProperty("cas.url"));
    ticketValidator.setAcceptAnyProxy(true);
    return ticketValidator;
  }

  @Bean
  public CasAuthenticationFilter casAuthenticationFilter(
      final AuthenticationManager authenticationManager) {
    final CasAuthenticationFilter casAuthenticationFilter = new CasAuthenticationFilter();
    casAuthenticationFilter.setAuthenticationManager(authenticationManager);
    casAuthenticationFilter.setFilterProcessesUrl("/j_spring_cas_security_check");
    return casAuthenticationFilter;
  }

  @Bean
  public SingleSignOutFilter singleSignOutFilter() {
    final SingleSignOutFilter singleSignOutFilter = new SingleSignOutFilter();
    singleSignOutFilter.setIgnoreInitConfiguration(true);
    return singleSignOutFilter;
  }

  @Bean
  public CasAuthenticationEntryPoint casAuthenticationEntryPoint() {
    final CasAuthenticationEntryPoint casAuthenticationEntryPoint =
        new CasAuthenticationEntryPoint();
    casAuthenticationEntryPoint.setLoginUrl(environment.getRequiredProperty("cas.login"));
    casAuthenticationEntryPoint.setServiceProperties(serviceProperties());
    return casAuthenticationEntryPoint;
  }

  @Bean
  public HttpSessionSecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository();
  }

  public static Customizer<
          AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry>
      nonAuthenticatedRoutes(
          final Collection<String> additionalGetPaths,
          final Collection<String> additionalPostPaths) {
    final Stream<String> commonGet =
        Stream.of(
            "/buildversion.txt",
            "/actuator/health",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger",
            "/swagger/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars/swagger-ui/**",
            "/cxf/rest/v1/localisation",
            "/cxf/rest/v1/localisation/",
            "/api/v1/localisation",
            "/api/v1/localisation/",
            "/api/v1/copy/localisation-files",
            "/api/v1/copy/available-namespaces",
            "/tolgee/*/*.json",
            "/tolgee/*/*/*.json",
            "/error",
            "/me.json");
    final List<String> allPaths = Stream.concat(commonGet, additionalGetPaths.stream()).toList();
    return (authz) -> {
      final AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
          registry =
              authz.requestMatchers(HttpMethod.GET, allPaths.toArray(String[]::new)).permitAll();
      if (!additionalPostPaths.isEmpty()) {
        registry
            .requestMatchers(HttpMethod.POST, additionalPostPaths.toArray(String[]::new))
            .permitAll();
      }
      registry.anyRequest().authenticated();
    };
  }

  public static Customizer<CorsConfigurer<HttpSecurity>> getCorsConfigurerCustomizer(
      final String[] allowOrigins) {
    return httpSecurityCorsConfigurer -> {
      final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
      final CorsConfiguration config = new CorsConfiguration();
      config.setAllowedOrigins(Arrays.asList(allowOrigins));
      config.setAllowCredentials(true);
      config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
      config.setAllowedHeaders(
          List.of(
              "X-PINGOTHER",
              "Origin",
              "X-Requested-With",
              "Content-Type",
              "Accept",
              "Caller-Id",
              "clientSubSystemCode",
              "CSRF",
              "ID"));
      config.setMaxAge(1728000L);

      source.registerCorsConfiguration("/api/v1/localisation**", config);
      source.registerCorsConfiguration("/cxf/rest/v1/localisation**", config);
      source.registerCorsConfiguration("/tolgee/**", config);
      httpSecurityCorsConfigurer.configurationSource(source);
    };
  }

  @Bean
  public SecurityFilterChain filterChain(
      final HttpSecurity http,
      final CasAuthenticationFilter casAuthenticationFilter,
      final SecurityContextRepository securityContextRepository,
      final AuthenticationEntryPoint authenticationEntryPoint)
      throws Exception {
    final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
    requestCache.setMatchingRequestParameterName(null);
    http.headers(HeadersConfigurer::disable)
        .csrf(CsrfConfigurer::disable)
        .cors(getCorsConfigurerCustomizer(allowOrigins))
        .securityMatcher("/**")
        .authorizeHttpRequests(
            nonAuthenticatedRoutes(Collections.emptyList(), Collections.emptyList()))
        .addFilterAt(casAuthenticationFilter, CasAuthenticationFilter.class)
        .addFilterBefore(singleSignOutFilter(), CasAuthenticationFilter.class)
        .securityContext(
            securityContext ->
                securityContext
                    .requireExplicitSave(true)
                    .securityContextRepository(securityContextRepository))
        .exceptionHandling(e -> e.authenticationEntryPoint(authenticationEntryPoint))
        .requestCache(cache -> cache.requestCache(requestCache));
    return http.build();
  }

  @Bean
  public AuthenticationManager authenticationManager(final HttpSecurity http) throws Exception {
    final AuthenticationManagerBuilder authenticationManagerBuilder =
        http.getSharedObject(AuthenticationManagerBuilder.class);
    return authenticationManagerBuilder.authenticationProvider(casAuthenticationProvider()).build();
  }
}
