package com.dtilt.pipeline.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dtilt.pipeline.BusinessRuleException;
import com.dtilt.pipeline.BusinessRuleException.Category;
import com.dtilt.pipeline.Pipeline;
import com.dtilt.pipeline.PipelineFactory;
import com.dtilt.pipeline.spring.web.PipelineWebAutoConfiguration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

class PipelineStarterIntegrationTests {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    PipelineAutoConfiguration.class, PipelineWebAutoConfiguration.class))
            .withUserConfiguration(ApplicationConfig.class);

    @Test
    void disabledStepIsSkippedAndBusinessFailureBecomesConflictResponse() {
        contextRunner.withPropertyValues("pipeline.disabled-steps=create-product.date-range").run(context -> {
            MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
            ExecutedSteps executed = context.getBean(ExecutedSteps.class);

            mvc.perform(post("/products").param("name", "widget"))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.code").value("PRODUCT_NAME_DUPLICATE"))
                    .andExpect(jsonPath("$.detail").value("A product named 'widget' already exists"))
                    .andExpect(jsonPath("$.details.name").value("widget"));

            assertThat(executed.names).containsExactly("normalize-name", "save-product");
        });
    }

    @Test
    void withoutTheToggleEveryStepRuns() {
        contextRunner.run(context -> {
            MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();

            mvc.perform(post("/products").param("name", "widget")).andExpect(status().isConflict());

            assertThat(context.getBean(ExecutedSteps.class).names)
                    .containsExactly("normalize-name", "date-range", "save-product");
        });
    }

    /** Nomi degli step eseguiti, nell'ordine di esecuzione. */
    static class ExecutedSteps {
        final List<String> names = new CopyOnWriteArrayList<>();
    }

    static class CreateProductContext {
        String name;

        CreateProductContext(String name) {
            this.name = name;
        }
    }

    @RestController
    static class ProductController {

        private final Pipeline<CreateProductContext> createProduct;

        ProductController(Pipeline<CreateProductContext> createProduct) {
            this.createProduct = createProduct;
        }

        @PostMapping("/products")
        String create(@RequestParam("name") String name) {
            return createProduct.run(new CreateProductContext(name)).name;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    static class ApplicationConfig {

        @Bean
        ExecutedSteps executedSteps() {
            return new ExecutedSteps();
        }

        @Bean
        Pipeline<CreateProductContext> createProductPipeline(PipelineFactory factory, ExecutedSteps executed) {
            return factory.<CreateProductContext>pipeline("create-product")
                    .step("normalize-name", context -> {
                        executed.names.add("normalize-name");
                        context.name = context.name.strip();
                    })
                    .step("date-range", context -> executed.names.add("date-range"))
                    .step("save-product", context -> {
                        executed.names.add("save-product");
                        throw new BusinessRuleException("PRODUCT_NAME_DUPLICATE", Category.CONFLICT,
                                "A product named '" + context.name + "' already exists",
                                Map.of("name", context.name));
                    })
                    .build();
        }

        @Bean
        ProductController productController(Pipeline<CreateProductContext> createProduct) {
            return new ProductController(createProduct);
        }
    }
}
