package com.dtilt.pipeline.exception;

import com.dtilt.pipeline.step.Step;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Errore di business atteso, lanciato da uno {@link Step} quando una regola non è rispettata.
 *
 * <p>La classe non è {@code final}: i progetti possono definire sottoclassi specifiche.
 */
public class BusinessRuleException extends PipelineException {

    private static final long serialVersionUID = 1L;

    /**
     * Categoria dell'errore, indipendente dal trasporto. Sono gli adapter (es. uno starter Spring)
     * a tradurla in uno status HTTP o in un comportamento di retry.
     */
    public enum Category {
        /** I dati in ingresso non sono validi. */
        INVALID_INPUT,
        /** L'operazione è in conflitto con lo stato corrente. */
        CONFLICT,
        /** Una risorsa richiesta non esiste. */
        NOT_FOUND,
        /** L'operazione non è permessa. */
        FORBIDDEN
    }

    private final String code;
    private final Category category;
    // La mappa è serializzabile; i valori lo sono solo se lo sono quelli forniti dal chiamante.
    @SuppressWarnings("serial")
    private final Map<String, Object> details;

    /**
     * Crea un errore di business senza dettagli aggiuntivi.
     *
     * @param code     codice stabile e leggibile da macchina; non null né blank
     * @param category categoria dell'errore; non null
     * @param message  messaggio leggibile; non null
     */
    public BusinessRuleException(String code, Category category, String message) {
        this(code, category, message, Map.of());
    }

    /**
     * Crea un errore di business con dettagli aggiuntivi.
     *
     * @param code     codice stabile e leggibile da macchina; non null né blank
     * @param category categoria dell'errore; non null
     * @param message  messaggio leggibile; non null
     * @param details  dettagli aggiuntivi, copiati in modo difensivo; non null
     */
    public BusinessRuleException(String code, Category category, String message,
                                 Map<String, Object> details) {
        super(Objects.requireNonNull(message, "message must not be null"));
        Objects.requireNonNull(code, "code must not be null");
        if (code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        this.code = code;
        this.category = Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(details, "details must not be null");
        this.details = Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }

    /**
     * Codice stabile e leggibile da macchina, es. {@code "PRODUCT_NAME_DUPLICATE"}.
     *
     * @return il codice dell'errore, mai null né blank
     */
    public String getCode() {
        return code;
    }

    /**
     * Categoria dell'errore.
     *
     * @return la categoria, mai null
     */
    public Category getCategory() {
        return category;
    }

    /**
     * Dettagli aggiuntivi.
     *
     * @return mappa non modificabile, mai null (vuota di default)
     */
    public Map<String, Object> getDetails() {
        return details;
    }
}
