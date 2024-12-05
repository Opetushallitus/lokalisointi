package fi.vm.sade.lokalisointi.configuration;

import ch.qos.logback.access.tomcat.LogbackValve;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "logback.access")
public class AccessLogConfiguration
    implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
  @Override
  public void customize(final TomcatServletWebServerFactory tomcat) {
    tomcat.addContextCustomizers(
        context -> {
          final LogbackValve logbackValve = new LogbackValve();
          logbackValve.setFilename("logback-access.xml");
          logbackValve.setAsyncSupported(true);
          context.getPipeline().addValve(logbackValve);
        });
  }
}
