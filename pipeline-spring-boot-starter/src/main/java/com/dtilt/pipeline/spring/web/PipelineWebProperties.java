package com.dtilt.pipeline.spring.web;

import com.dtilt.pipeline.exception.BusinessRuleException;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration of HTTP error handling, read from the {@code pipeline.web} properties. */
@ConfigurationProperties("pipeline.web")
public class PipelineWebProperties {

    /**
     * Adds pipeline and step to business error responses.
     */
    private boolean includeLocation = false;

    /**
     * Overrides, entry by entry, the default mapping from category to HTTP status (400-599).
     */
    private Map<BusinessRuleException.Category, Integer> statusMapping =
            new EnumMap<>(BusinessRuleException.Category.class);

    private final ExceptionHandler exceptionHandler = new ExceptionHandler();

    /**
     * Tells whether business error responses include {@code pipeline} and {@code step}.
     *
     * @return {@code true} if the location is included; the default is {@code false}
     */
    public boolean isIncludeLocation() {
        return includeLocation;
    }

    /**
     * Sets whether to include {@code pipeline} and {@code step} in business error responses.
     *
     * @param includeLocation {@code true} to include the location
     */
    public void setIncludeLocation(boolean includeLocation) {
        this.includeLocation = includeLocation;
    }

    /**
     * The custom mapping from category to HTTP status.
     *
     * @return the configured entries, never null (empty by default)
     */
    public Map<BusinessRuleException.Category, Integer> getStatusMapping() {
        return statusMapping;
    }

    /**
     * Sets the custom mapping from category to HTTP status.
     *
     * @param statusMapping the status by category; null means an empty map
     */
    public void setStatusMapping(Map<BusinessRuleException.Category, Integer> statusMapping) {
        this.statusMapping = (statusMapping != null)
                ? statusMapping
                : new EnumMap<>(BusinessRuleException.Category.class);
    }

    /**
     * The configuration of the default HTTP handler.
     *
     * @return the handler configuration, never null
     */
    public ExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    /** Properties with the {@code pipeline.web.exception-handler} prefix. */
    public static class ExceptionHandler {

        /**
         * Enables the default HTTP handler for pipeline exceptions.
         */
        private boolean enabled = true;

        /**
         * Tells whether the default HTTP handler is enabled.
         *
         * @return {@code true} if enabled (the default)
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Enables or disables the default HTTP handler.
         *
         * @param enabled {@code false} to not register {@code PipelineExceptionHandler}
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
