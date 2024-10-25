package fi.vm.sade.lokalisointi.configuration

import fi.vm.sade.valinta.dokumenttipalvelu.Dokumenttipalvelu
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.data.relational.core.mapping.event.RelationalEvent
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableJdbcRepositories
@EnableTransactionManagement
class PersistenceConfiguration : AbstractJdbcConfiguration() {
    @Bean
    fun loggingListener(): ApplicationListener<*> {
        return ApplicationListener { event: ApplicationEvent? ->
            if (event is RelationalEvent<*>) {
                LOG.debug("Received a spring data event: {}", event)
            }
        }
    }

    @Bean
    @Profile("default")
    fun dokumenttipalvelu(
        @Value("\${aws.region}") region: String?,
        @Value("\${aws.bucket.name}") bucketName: String?
    ): Dokumenttipalvelu {
        return Dokumenttipalvelu(region, bucketName)
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(PersistenceConfiguration::class.java)
    }
}
