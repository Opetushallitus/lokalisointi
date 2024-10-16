package fi.vm.sade.lokalisointi.configuration;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServletContainerConfiguration {

  /**
   * Konfiguraatio kun palvelua ajetaan HTTPS proxyn läpi. Käytännössä tämä muuttaa {@link
   * javax.servlet.ServletRequest#getScheme()} palauttamaan `https` jolloin palvelun kautta luodut
   * urlit muodostuvat oikein.
   *
   * <p>Aktivointi: `lokalistointi.uses-ssl-proxy` arvoon `true`.
   *
   * @return EmbeddedServletContainerCustomizer jonka Spring automaattisesti tunnistaa ja lisää
   *     servlet containerin konfigurointiin
   */
  @Bean
  @ConditionalOnProperty("lokalisointi.uses-ssl-proxy")
  public WebServerFactoryCustomizer<?> sslProxyCustomizer() {
    return (WebServerFactory container) -> {
      if (container instanceof ConfigurableServletWebServerFactory) {
        final TomcatServletWebServerFactory tomcat = (TomcatServletWebServerFactory) container;
        tomcat.addConnectorCustomizers(
            (Connector connector) -> {
              connector.setScheme("https");
              connector.setSecure(true);
            });
      }
    };
  }
}
