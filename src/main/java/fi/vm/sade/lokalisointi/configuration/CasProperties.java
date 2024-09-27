package fi.vm.sade.lokalisointi.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cas-service")
public class CasProperties {
  private String service;
  private Boolean sendRenew;
  private String key;
  private String fallbackUserDetailsProviderUrl;

  public String getService() {
    return service;
  }

  public void setService(String service) {
    this.service = service;
  }

  public Boolean getSendRenew() {
    return sendRenew;
  }

  public void setSendRenew(Boolean sendRenew) {
    this.sendRenew = sendRenew;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getFallbackUserDetailsProviderUrl() {
    return fallbackUserDetailsProviderUrl;
  }

  public void setFallbackUserDetailsProviderUrl(String fallbackUserDetailsProviderUrl) {
    this.fallbackUserDetailsProviderUrl = fallbackUserDetailsProviderUrl;
  }

  public String toString() {
    return String.format(
        "service: %s, sendRenew: %s, key: %s, fallbackUserDetailsProviderUrl: %s",
        service, sendRenew, key, fallbackUserDetailsProviderUrl);
  }
}
