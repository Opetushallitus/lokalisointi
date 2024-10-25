package fi.vm.sade.lokalisointi.configuration

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfiguration : WebMvcConfigurer {
    @Bean
    fun lokalisointiAPI(): OpenAPI {
        return OpenAPI()
            .info(Info().title("Lokalisointi API").description("Lokalisointi").version("v1.0"))
    }

    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.addRedirectViewController("/swagger/**", "/swagger-ui/index.html")
        registry.addRedirectViewController("/swagger", "/swagger-ui/index.html")
        registry.addRedirectViewController("/", "/secured/index.html")
        registry.addRedirectViewController("/index.html", "/secured/index.html")
    }
}
