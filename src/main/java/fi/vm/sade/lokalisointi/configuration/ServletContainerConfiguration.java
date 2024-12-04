package fi.vm.sade.lokalisointi.configuration;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.valves.rewrite.RewriteValve;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

@Configuration
@Profile("!dev")
@ConditionalOnProperty("lokalisointi.uses-ssl-proxy")
public class ServletContainerConfiguration
    implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
  private static final Logger LOG = LoggerFactory.getLogger(ServletContainerConfiguration.class);

  /**
   * Konfiguraatio kun palvelua ajetaan HTTPS proxyn läpi. Käytännössä tämä muuttaa {@link
   * javax.servlet.ServletRequest#getScheme()} palauttamaan `https` jolloin palvelun kautta luodut
   * urlit muodostuvat oikein.
   *
   * <p>Aktivointi: `lokalistointi.uses-ssl-proxy` arvoon `true`.
   */
  @Override
  public void customize(final TomcatServletWebServerFactory server) {
    server.addConnectorCustomizers(
        (Connector connector) -> {
          connector.setScheme("https");
          connector.setSecure(true);
        });
  }
}
