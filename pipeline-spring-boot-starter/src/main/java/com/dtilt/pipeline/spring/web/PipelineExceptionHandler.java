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
 * Traduce le eccezioni della pipeline in risposte {@link ProblemDetail} (RFC 9457).
 *
 * <p>Una {@link BusinessRuleException} diventa una risposta 4xx con il codice e i dettagli
 * dell'errore; una {@link StepExecutionException} diventa un 500 generico, senza esporre al
 * client messaggio, causa, pipeline o step, e viene loggata a livello {@code ERROR}.
 *
 * <p>La classe è estendibile: un progetto personalizza la risposta sovrascrivendo i metodi
 * {@code protected} e registrando la sottoclasse come bean, che sostituisce quello di default.
 * L'ordine {@code LOWEST_PRECEDENCE - 100} fa sì che un advice del progetto senza {@code @Order}
 * con un catch-all su {@code Exception} non intercetti le eccezioni della pipeline.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class PipelineExceptionHandler {

    private static final Log logger = LogFactory.getLog(PipelineExceptionHandler.class);

    private final boolean includeLocation;
    private final Map<BusinessRuleException.Category, HttpStatusCode> statusByCategory;

    /**
     * Crea l'handler.
     *
     * @param properties configurazione della gestione HTTP degli errori
     * @throws PipelineConfigurationException se {@code pipeline.web.status-mapping} contiene uno
     *                                        status fuori dall'intervallo 400-599
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
     * Gestisce un errore di business atteso. Non viene loggato.
     *
     * @param e eccezione lanciata dalla pipeline
     * @return la risposta costruita da {@link #businessProblem(BusinessRuleException)}
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException e) {
        return businessProblem(e);
    }

    /**
     * Gestisce un errore tecnico inatteso: lo logga a livello {@code ERROR}, con pipeline, step e
     * stack trace, e restituisce una risposta generica.
     *
     * @param e eccezione lanciata dalla pipeline
     * @return la risposta costruita da {@link #technicalProblem(StepExecutionException)}
     */
    @ExceptionHandler(StepExecutionException.class)
    public ProblemDetail handleStepExecution(StepExecutionException e) {
        logger.error("Step '" + e.getStep() + "' of pipeline '" + e.getPipeline() + "' failed", e);
        return technicalProblem(e);
    }

    /**
     * Status HTTP per una categoria di errore di business: quello configurato in
     * {@code pipeline.web.status-mapping}, altrimenti il default (400, 403, 404, 409).
     *
     * @param category categoria dell'errore
     * @return lo status HTTP della risposta
     */
    protected HttpStatusCode statusFor(BusinessRuleException.Category category) {
        return statusByCategory.get(category);
    }

    /**
     * Costruisce la risposta per un errore di business: status dalla categoria, {@code detail}
     * dal messaggio, property {@code code}, {@code details} se non vuota e, se
     * {@code pipeline.web.include-location=true}, {@code pipeline} e {@code step}.
     *
     * @param e eccezione lanciata dalla pipeline
     * @return la risposta di errore
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
     * Costruisce la risposta generica per un errore tecnico: 500, senza alcun dettaglio
     * dell'eccezione.
     *
     * @param e eccezione lanciata dalla pipeline
     * @return la risposta di errore
     */
    protected ProblemDetail technicalProblem(StepExecutionException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred.");
        problem.setTitle(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        problem.setProperty("code", "INTERNAL_ERROR");
        return problem;
    }
}
