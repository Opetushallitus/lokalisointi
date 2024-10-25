package fi.vm.sade.lokalisointi.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "cas-service")
class CasProperties {
    var service: String? = null
    var sendRenew: Boolean? = null
    var key: String? = null
    var fallbackUserDetailsProviderUrl: String? = null

    override fun toString(): String {
        return String.format(
            "service: %s, sendRenew: %s, key: %s, fallbackUserDetailsProviderUrl: %s",
            service, sendRenew, key, fallbackUserDetailsProviderUrl
        )
    }
}
