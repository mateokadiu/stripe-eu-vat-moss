package io.github.mateokadiu.moss.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Spring Boot entrypoint for the REST + Actuator surface. */
@SpringBootApplication
public class MossApplication {

  public static void main(String[] args) {
    SpringApplication.run(MossApplication.class, args);
  }
}
