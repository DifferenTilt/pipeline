package io.github.differentilt.pipeline.exception;

import io.github.differentilt.pipeline.step.Step;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An expected business error, thrown by a {@link Step} when a rule is not satisfied.
 *
 * <p>The class is not {@code final}: projects can define their own specific subclasses.
 */
public class BusinessRuleException extends PipelineException {

    private static final long serialVersionUID = 1L;

    /**
     * The error category, independent of the transport. It is up to adapters (e.g. a Spring
     * starter) to translate it into an HTTP status or a retry behaviour.
     */
    public enum Category {
        /** The input data is not valid. */
        INVALID_INPUT,
        /** The operation conflicts with the current state. */
        CONFLICT,
        /** A requested resource does not exist. */
        NOT_FOUND,
        /** The operation is not allowed. */
        FORBIDDEN
    }

    /** The stable, machine-readable error code. */
    private final String code;
    /** The error category. */
    private final Category category;
    /** The additional details, as an unmodifiable copy of the ones supplied by the caller. */
    // The map is serializable; its values are only if the ones supplied by the caller are.
    @SuppressWarnings("serial")
    private final Map<String, Object> details;

    /**
     * Creates a business error without additional details.
     *
     * @param code     a stable, machine-readable code; not null or blank
     * @param category the error category; not null
     * @param message  a human-readable message; not null
     */
    public BusinessRuleException(String code, Category category, String message) {
        this(code, category, message, Map.of());
    }

    /**
     * Creates a business error with additional details.
     *
     * @param code     a stable, machine-readable code; not null or blank
     * @param category the error category; not null
     * @param message  a human-readable message; not null
     * @param details  additional details, defensively copied; not null
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
     * A stable, machine-readable code, e.g. {@code "PRODUCT_NAME_DUPLICATE"}.
     *
     * @return the error code, never null or blank
     */
    public String getCode() {
        return code;
    }

    /**
     * The error category.
     *
     * @return the category, never null
     */
    public Category getCategory() {
        return category;
    }

    /**
     * Additional details.
     *
     * @return an unmodifiable map, never null (empty by default)
     */
    public Map<String, Object> getDetails() {
        return details;
    }
}
