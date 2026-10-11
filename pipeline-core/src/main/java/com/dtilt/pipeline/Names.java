package com.dtilt.pipeline;

import java.util.Objects;
import java.util.regex.Pattern;

/** Validation of pipeline and step names. */
final class Names {

    private static final Pattern KEBAB_CASE = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    private Names() {
    }

    /**
     * Checks that a name is kebab-case.
     *
     * @param value the name to check
     * @param what  what the name refers to, used in the error message (e.g. "step name")
     * @return {@code value}
     * @throws NullPointerException     if {@code value} is null
     * @throws IllegalArgumentException if {@code value} is not kebab-case
     */
    static String requireKebabCase(String value, String what) {
        Objects.requireNonNull(value, what + " must not be null");
        if (!KEBAB_CASE.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    what + " must be kebab-case (" + KEBAB_CASE.pattern() + "): '" + value + "'");
        }
        return value;
    }
}
