package fi.vm.sade.lokalisointi.api;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

@RestController
public class CustomErrorController implements ErrorController {
  private static final String ERROR_PATH = "/error";

  @RequestMapping(value = ERROR_PATH)
  public Map<String, Object> handleError(final WebRequest request) {
    return new DefaultErrorAttributes()
        .getErrorAttributes(request, ErrorAttributeOptions.defaults());
  }
}
