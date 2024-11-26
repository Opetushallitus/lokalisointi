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
public class ServletContainerConfiguration {
  private static final Logger LOG = LoggerFactory.getLogger(ServletContainerConfiguration.class);

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

  /** Redirect calls to /virkailija-raamit to QA when running with dev profile */
  @Bean
  @Profile("dev")
  public WebServerFactoryCustomizer<?> rewriteInitializerCustomizer() {
    return (WebServerFactory container) -> {
      if (container instanceof ConfigurableServletWebServerFactory) {
        final TomcatServletWebServerFactory tomcat = (TomcatServletWebServerFactory) container;
        try {
          // Using Tomcat's engine valve (outside Spring container) for rewrites, so need to create
          // config directory manually
          final File tempDirectory = Files.createTempDirectory("tomcat").toFile();
          tempDirectory.deleteOnExit();
          new File(tempDirectory, "conf/Tomcat").mkdirs();
          tomcat.setBaseDirectory(tempDirectory);
          final File target =
              new File(
                  """
              %s/conf/Tomcat/rewrite.config"""
                      .formatted(tempDirectory.getAbsolutePath()));
          LOG.info("Copying rewrite.config to: {}", target);
          IOUtils.copy(
              new FileInputStream("src/test/resources/rewrite.config"),
              new FileOutputStream(target));
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        final RewriteValve valve = new RewriteValve();
        LOG.info("Adding rewrite valve: {}", valve);
        tomcat.addEngineValves(valve);
      }
    };
  }
}
