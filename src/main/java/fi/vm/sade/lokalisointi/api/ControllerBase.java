package fi.vm.sade.lokalisointi.api;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ControllerBase {
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler({
    DbActionExecutionException.class,
    IllegalArgumentException.class,
    HttpMessageNotReadableException.class
  })
  public Map<String, ?> handleUserErrors(final RuntimeException ex) {
    return Map.of(
        "error",
        Stream.of(
                Optional.of(new ImmutablePair<>("message", ex.getMessage())),
                Optional.ofNullable(ex.getCause())
                    .map(c -> new ImmutablePair<>("cause", c.getMessage())))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toMap(ImmutablePair::getLeft, ImmutablePair::getRight)));
  }
}