package fi.vm.sade.lokalisointi.configuration;

import fi.vm.sade.valinta.dokumenttipalvelu.Dokumenttipalvelu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.relational.core.mapping.event.RelationalEvent;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJdbcRepositories
@EnableTransactionManagement
public class PersistenceConfiguration extends AbstractJdbcConfiguration {
  private static final Logger LOG = LoggerFactory.getLogger(PersistenceConfiguration.class);

  @Bean
  public ApplicationListener<?> loggingListener() {
    return (ApplicationListener<ApplicationEvent>)
        event -> {
          if (event instanceof RelationalEvent) {
            LOG.debug("Received a spring data event: {}", event);
          }
        };
  }

  @Bean
  @Profile("!dev")
  public Dokumenttipalvelu dokumenttipalvelu(
      @Value("${aws.region}") final String region,
      @Value("${aws.bucket.name}") final String bucketName) {
    return new Dokumenttipalvelu(region, bucketName);
  }
}
