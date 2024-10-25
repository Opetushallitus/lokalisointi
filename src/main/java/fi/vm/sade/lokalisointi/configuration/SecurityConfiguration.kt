package fi.vm.sade.lokalisointi.configuration

import fi.vm.sade.javautils.kayttooikeusclient.OphUserDetailsServiceImpl
import org.apereo.cas.client.session.SingleSignOutFilter
import org.apereo.cas.client.validation.Cas20ProxyTicketValidator
import org.apereo.cas.client.validation.TicketValidator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.cas.ServiceProperties
import org.springframework.security.cas.authentication.CasAuthenticationProvider
import org.springframework.security.cas.web.CasAuthenticationEntryPoint
import org.springframework.security.cas.web.CasAuthenticationFilter
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.*
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextRepository
import java.util.*

@Profile("default", "dev")
@Configuration
@EnableMethodSecurity(securedEnabled = true)
@EnableWebSecurity
class SecurityConfiguration @Autowired constructor(
    private val casProperties: CasProperties,
    private val environment: Environment
) {
    init {
        LOG.info("CAS props: {}", casProperties)
    }

    @Bean
    fun serviceProperties(): ServiceProperties {
        val serviceProperties = ServiceProperties()
        serviceProperties.service = casProperties.service + "/j_spring_cas_security_check"
        serviceProperties.isSendRenew = casProperties.sendRenew!!
        serviceProperties.isAuthenticateAllArtifacts = true
        return serviceProperties
    }

    @Bean
    fun casAuthenticationProvider(): CasAuthenticationProvider {
        val casAuthenticationProvider = CasAuthenticationProvider()
        casAuthenticationProvider.setAuthenticationUserDetailsService(OphUserDetailsServiceImpl())
        casAuthenticationProvider.setServiceProperties(serviceProperties())
        casAuthenticationProvider.setTicketValidator(ticketValidator())
        casAuthenticationProvider.setKey(casProperties.key)
        return casAuthenticationProvider
    }

    @Bean
    fun ticketValidator(): TicketValidator {
        val ticketValidator =
            Cas20ProxyTicketValidator(environment.getRequiredProperty("cas.url"))
        ticketValidator.setAcceptAnyProxy(true)
        return ticketValidator
    }

    @Bean
    fun casAuthenticationFilter(
        authenticationManager: AuthenticationManager?
    ): CasAuthenticationFilter {
        val casAuthenticationFilter = CasAuthenticationFilter()
        casAuthenticationFilter.setAuthenticationManager(authenticationManager)
        casAuthenticationFilter.setFilterProcessesUrl("/j_spring_cas_security_check")
        return casAuthenticationFilter
    }

    @Bean
    fun singleSignOutFilter(): SingleSignOutFilter {
        val singleSignOutFilter = SingleSignOutFilter()
        singleSignOutFilter.setIgnoreInitConfiguration(true)
        return singleSignOutFilter
    }

    @Bean
    fun casAuthenticationEntryPoint(): CasAuthenticationEntryPoint {
        val casAuthenticationEntryPoint =
            CasAuthenticationEntryPoint()
        casAuthenticationEntryPoint.loginUrl = environment.getRequiredProperty("cas.login")
        casAuthenticationEntryPoint.serviceProperties = serviceProperties()
        return casAuthenticationEntryPoint
    }

    @Bean
    fun securityContextRepository(): HttpSessionSecurityContextRepository {
        return HttpSessionSecurityContextRepository()
    }

    @Bean
    fun filterChain(
        http: HttpSecurity,
        casAuthenticationFilter: CasAuthenticationFilter,
        securityContextRepository: SecurityContextRepository?,
        authenticationEntryPoint: AuthenticationEntryPoint?
    ): SecurityFilterChain {
        http.headers { obj: HeadersConfigurer<HttpSecurity> -> obj.disable() }
            .csrf { obj: CsrfConfigurer<HttpSecurity> -> obj.disable() }
            .securityMatcher("/**")
            .authorizeHttpRequests(nonAuthenticatedRoutes())
            .addFilterAt(casAuthenticationFilter, CasAuthenticationFilter::class.java)
            .addFilterBefore(singleSignOutFilter(), CasAuthenticationFilter::class.java)
            .securityContext { securityContext: SecurityContextConfigurer<HttpSecurity?> ->
                securityContext
                    .requireExplicitSave(true)
                    .securityContextRepository(securityContextRepository)
            }
            .exceptionHandling { e: ExceptionHandlingConfigurer<HttpSecurity?> ->
                e.authenticationEntryPoint(
                    authenticationEntryPoint
                )
            }
        return http.build()
    }

    @Bean
    fun authenticationManager(http: HttpSecurity): AuthenticationManager {
        val authenticationManagerBuilder =
            http.getSharedObject(AuthenticationManagerBuilder::class.java)
        return authenticationManagerBuilder.authenticationProvider(casAuthenticationProvider()).build()
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(SecurityConfiguration::class.java)
        fun nonAuthenticatedRoutes(vararg additionalPaths: String): Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry> {
            val additional = additionalPaths.toList()
            val common =
                listOf(
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
                    "/api/v1/localisation",
                    "/api/v1/copy/localisation-files",
                    "/api/v1/copy/available-namespaces"
                )
            val allPaths = (common + additional).toTypedArray()
            return Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry> { authz: AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry ->
                authz
                    .requestMatchers(HttpMethod.GET, *allPaths)
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }
        }
    }
}
