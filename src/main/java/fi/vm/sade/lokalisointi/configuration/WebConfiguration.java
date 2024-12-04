package fi.vm.sade.lokalisointi.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.*;

import java.util.Arrays;
import java.util.concurrent.Executor;

@Configuration
@EnableWebMvc
@EnableAsync
public class WebConfiguration implements WebMvcConfigurer, AsyncConfigurer {
  private static final Logger LOG = LoggerFactory.getLogger(WebConfiguration.class);
  @Autowired private Environment env;

  @Bean
  public OpenAPI lokalisointiAPI() {
    return new OpenAPI()
        .info(new Info().title("Lokalisointi API").description("Lokalisointi").version("v1.0"));
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/swagger/**")
        .addResourceLocations("redirect:/swagger-ui/index.html");
    registry.addResourceHandler("/swagger").addResourceLocations("redirect:/swagger-ui/index.html");
    registry.addResourceHandler("/index.html").addResourceLocations("redirect:/secured/index.html");
    registry.addResourceHandler("/secured/**").addResourceLocations("classpath:/static/secured/");
    registry
        .addResourceHandler("/buildversion.txt")
        .addResourceLocations("classpath:/static/buildversion.txt");
    if (env != null && Arrays.asList(env.getActiveProfiles()).contains("dev")) {
      LOG.info("Mounting /me.json in dev");
      registry.addResourceHandler("/me.json").addResourceLocations("classpath:/static/me.json");
    }
  }

  @Override
  public Executor getAsyncExecutor() {
    final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(7);
    executor.setMaxPoolSize(42);
    executor.setQueueCapacity(11);
    executor.setThreadNamePrefix("SpringAsync-");
    executor.initialize();
    LOG.info("Created async executor {}", executor);
    return executor;
  }

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return new SimpleAsyncUncaughtExceptionHandler();
  }
}
