package com.dtilt.pipeline.spring.web;

import com.dtilt.pipeline.exception.BusinessRuleException;
import com.dtilt.pipeline.exception.StepExecutionException;
import com.dtilt.pipeline.spring.PipelineConfigurationException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates pipeline exceptions into {@link ProblemDetail} responses (RFC 9457).
 *
 * <p>A {@link BusinessRuleException} becomes a 4xx response with the error code and details; a
 * {@link StepExecutionException} becomes a generic 500, without exposing the message, cause,
 * pipeline or step to the client, and is logged at {@code ERROR}.
 *
 * <p>The class can be extended: a project customises the response by overriding the
 * {@code protected} methods and registering the subclass as a bean, which replaces the default
 * one. The {@code LOWEST_PRECEDENCE - 100} order means that a project advice without
 * {@code @Order} and with a catch-all on {@code Exception} does not intercept pipeline
 * exceptions.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class PipelineExceptionHandler {

    private static final Log logger = LogFactory.getLog(PipelineExceptionHandler.class);

    private final boolean includeLocation;
    private final Map<BusinessRuleException.Category, HttpStatusCode> statusByCategory;

    /**
     * Creates the handler.
     *
     * @param properties the configuration of HTTP error handling
     * @throws PipelineConfigurationException if {@code pipeline.web.status-mapping} contains a
     *                                        status outside the 400-599 range
     */
    public PipelineExceptionHandler(PipelineWebProperties properties) {
        Objects.requireNonNull(properties, "properties must not be null");
        this.includeLocation = properties.isIncludeLocation();
        this.statusByCategory = new EnumMap<>(BusinessRuleException.Category.class);
        this.statusByCategory.put(BusinessRuleException.Category.INVALID_INPUT, HttpStatus.BAD_REQUEST);
        this.statusByCategory.put(BusinessRuleException.Category.FORBIDDEN, HttpStatus.FORBIDDEN);
        this.statusByCategory.put(BusinessRuleException.Category.NOT_FOUND, HttpStatus.NOT_FOUND);
        this.statusByCategory.put(BusinessRuleException.Category.CONFLICT, HttpStatus.CONFLICT);
        properties.getStatusMapping().forEach((category, status) -> {
            if (status == null || status < 400 || status > 599) {
                throw new PipelineConfigurationException("Invalid pipeline.web.status-mapping."
                        + category + ": " + status + " (must be between 400 and 599)");
            }
            this.statusByCategory.put(category, HttpStatusCode.valueOf(status));
        });
    }

    /**
     * Handles an expected business error. It is not logged.
     *
     * @param e the exception thrown by the pipeline
     * @return the response built by {@link #businessProblem(BusinessRuleException)}
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException e) {
        return businessProblem(e);
    }

    /**
     * Handles an unexpected technical error: logs it at {@code ERROR}, with pipeline, step and
     * stack trace, and returns a generic response.
     *
     * @param e the exception thrown by the pipeline
     * @return the response built by {@link #technicalProblem(StepExecutionException)}
     */
    @ExceptionHandler(StepExecutionException.class)
    public ProblemDetail handleStepExecution(StepExecutionException e) {
        logger.error("Step '" + e.getStep() + "' of pipeline '" + e.getPipeline() + "' failed", e);
        return technicalProblem(e);
    }

    /**
     * The HTTP status for a business error category: the one configured in
     * {@code pipeline.web.status-mapping}, otherwise the default (400, 403, 404, 409).
     *
     * @param category the error category
     * @return the HTTP status of the response
     */
    protected HttpStatusCode statusFor(BusinessRuleException.Category category) {
        return statusByCategory.get(category);
    }

    /**
     * Builds the response for a business error: status from the category, {@code detail} from
     * the message, the {@code code} property, {@code details} when not empty and, if
     * {@code pipeline.web.include-location=true}, {@code pipeline} and {@code step}.
     *
     * @param e the exception thrown by the pipeline
     * @return the error response
     */
    protected ProblemDetail businessProblem(BusinessRuleException e) {
        HttpStatusCode status = statusFor(e.getCategory());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        HttpStatus knownStatus = HttpStatus.resolve(status.value());
        if (knownStatus != null) {
            problem.setTitle(knownStatus.getReasonPhrase());
        }
        problem.setProperty("code", e.getCode());
        if (!e.getDetails().isEmpty()) {
            problem.setProperty("details", e.getDetails());
        }
        if (includeLocation) {
            if (e.getPipeline() != null) {
                problem.setProperty("pipeline", e.getPipeline());
            }
            if (e.getStep() != null) {
                problem.setProperty("step", e.getStep());
            }
        }
        return problem;
    }

    /**
     * Builds the generic response for a technical error: 500, without any detail of the
     * exception.
     *
     * @param e the exception thrown by the pipeline
     * @return the error response
     */
    protected ProblemDetail technicalProblem(StepExecutionException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred.");
        problem.setTitle(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        problem.setProperty("code", "INTERNAL_ERROR");
        return problem;
    }
}
