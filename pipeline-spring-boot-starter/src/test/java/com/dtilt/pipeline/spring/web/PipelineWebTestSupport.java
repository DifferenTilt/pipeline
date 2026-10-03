package com.dtilt.pipeline.spring.web;

import com.dtilt.pipeline.exception.BusinessRuleException;
import com.dtilt.pipeline.exception.BusinessRuleException.Category;
import com.dtilt.pipeline.Pipeline;
import com.dtilt.pipeline.step.Step;
import java.util.Map;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/** Configurazione, controller e advice condivisi dai test web. */
final class PipelineWebTestSupport {

    private PipelineWebTestSupport() {
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    static class WebMvcConfig {
    }

    /** Controller che lancia l'eccezione richiesta dall'interno di una pipeline. */
    @RestController
    static class FailingController {

        private static void runFailing(Step<Object> failingStep) {
            Pipeline.builder("test-pipeline").step("failing-step", failingStep).build().run(new Object());
        }

        @GetMapping("/business")
        String business(@RequestParam("category") Category category,
                        @RequestParam(name = "details", defaultValue = "false") boolean details) {
            runFailing(context -> {
                throw details
                        ? new BusinessRuleException("TEST_CODE", category, "business failure",
                                Map.of("name", "widget"))
                        : new BusinessRuleException("TEST_CODE", category, "business failure");
            });
            return "unreachable";
        }

        @GetMapping("/technical")
        String technical() {
            runFailing(context -> {
                throw new IllegalStateException("secret cause message");
            });
            return "unreachable";
        }

        @GetMapping("/other")
        String other() {
            throw new IllegalArgumentException("not a pipeline exception");
        }
    }

    /** Advice del progetto senza {@code @Order}, con un catch-all su {@code Exception}. */
    @RestControllerAdvice
    static class CatchAllAdvice {

        @ExceptionHandler(Exception.class)
        ResponseEntity<String> handle(Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("catch-all");
        }
    }
}
