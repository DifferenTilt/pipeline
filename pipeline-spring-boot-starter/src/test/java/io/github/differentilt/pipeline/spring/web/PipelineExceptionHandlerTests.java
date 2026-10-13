package io.github.differentilt.pipeline.spring.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.differentilt.pipeline.exception.BusinessRuleException.Category;
import io.github.differentilt.pipeline.spring.PipelineAutoConfiguration;
import io.github.differentilt.pipeline.spring.web.PipelineWebTestSupport.CatchAllAdvice;
import io.github.differentilt.pipeline.spring.web.PipelineWebTestSupport.FailingController;
import io.github.differentilt.pipeline.spring.web.PipelineWebTestSupport.WebMvcConfig;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(OutputCaptureExtension.class)
class PipelineExceptionHandlerTests {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    PipelineAutoConfiguration.class, PipelineWebAutoConfiguration.class))
            .withUserConfiguration(WebMvcConfig.class, FailingController.class);

    private static MockMvc mockMvc(WebApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void eachCategoryMapsToItsDefaultStatus() {
        Map<Category, Integer> expected = Map.of(
                Category.INVALID_INPUT, 400,
                Category.FORBIDDEN, 403,
                Category.NOT_FOUND, 404,
                Category.CONFLICT, 409);
        assertThat(expected).containsOnlyKeys(Category.values());

        contextRunner.run(context -> {
            MockMvc mvc = mockMvc(context);
            for (Map.Entry<Category, Integer> entry : expected.entrySet()) {
                mvc.perform(get("/business").param("category", entry.getKey().name()))
                        .andExpect(status().is(entry.getValue()))
                        .andExpect(jsonPath("$.status").value(entry.getValue()));
            }
        });
    }

    @Test
    void businessProblemBodyHasDetailCodeAndDetails() {
        contextRunner.run(context -> mockMvc(context)
                .perform(get("/business").param("category", "CONFLICT").param("details", "true"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("business failure"))
                .andExpect(jsonPath("$.code").value("TEST_CODE"))
                .andExpect(jsonPath("$.details.name").value("widget")));
    }

    @Test
    void detailsAreAbsentWhenEmpty() {
        contextRunner.run(context -> mockMvc(context)
                .perform(get("/business").param("category", "CONFLICT"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("TEST_CODE"))
                .andExpect(jsonPath("$.details").doesNotExist()));
    }

    @Test
    void locationIsAbsentByDefault() {
        contextRunner.run(context -> mockMvc(context)
                .perform(get("/business").param("category", "NOT_FOUND"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.pipeline").doesNotExist())
                .andExpect(jsonPath("$.step").doesNotExist()));
    }

    @Test
    void locationIsPresentWhenIncludeLocationIsTrue() {
        contextRunner.withPropertyValues("pipeline.web.include-location=true").run(context -> mockMvc(context)
                .perform(get("/business").param("category", "NOT_FOUND"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.pipeline").value("test-pipeline"))
                .andExpect(jsonPath("$.step").value("failing-step")));
    }

    @Test
    void statusMappingOverridesOnlyTheConfiguredCategory() {
        contextRunner.withPropertyValues("pipeline.web.status-mapping.INVALID_INPUT=422").run(context -> {
            MockMvc mvc = mockMvc(context);
            mvc.perform(get("/business").param("category", "INVALID_INPUT"))
                    .andExpect(status().is(422))
                    .andExpect(jsonPath("$.status").value(422))
                    .andExpect(jsonPath("$.code").value("TEST_CODE"));
            mvc.perform(get("/business").param("category", "FORBIDDEN")).andExpect(status().isForbidden());
            mvc.perform(get("/business").param("category", "NOT_FOUND")).andExpect(status().isNotFound());
            mvc.perform(get("/business").param("category", "CONFLICT")).andExpect(status().isConflict());
        });
    }

    @Test
    void businessErrorsAreNotLogged(CapturedOutput output) {
        contextRunner.run(context -> {
            mockMvc(context).perform(get("/business").param("category", "CONFLICT"))
                    .andExpect(status().isConflict());

            assertThat(output).doesNotContain("business failure").doesNotContain("TEST_CODE");
        });
    }

    @Test
    void technicalProblemIsGenericAndDoesNotLeakInternals(CapturedOutput output) {
        contextRunner.withPropertyValues("pipeline.web.include-location=true").run(context -> {
            String body = mockMvc(context).perform(get("/technical"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.title").value("Internal Server Error"))
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.detail").value("An internal error occurred."))
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                    .andReturn().getResponse().getContentAsString();

            assertThat(body)
                    .doesNotContain("secret cause message")
                    .doesNotContain("test-pipeline")
                    .doesNotContain("failing-step");
            assertThat(output)
                    .contains("ERROR")
                    .contains("Step 'failing-step' of pipeline 'test-pipeline' failed")
                    .contains("java.lang.IllegalStateException: secret cause message");
        });
    }

    @Test
    void unorderedCatchAllAdviceDoesNotInterceptPipelineExceptions() {
        contextRunner.withUserConfiguration(CatchAllAdvice.class).run(context -> {
            MockMvc mvc = mockMvc(context);
            mvc.perform(get("/business").param("category", "CONFLICT"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("TEST_CODE"));
            mvc.perform(get("/technical"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
            mvc.perform(get("/other"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().string("catch-all"));
        });
    }
}
