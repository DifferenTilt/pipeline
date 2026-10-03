package com.dtilt.pipeline.step;

import com.dtilt.pipeline.Pipeline;
import com.dtilt.pipeline.exception.BusinessRuleException;

/**
 * Un'unità di logica di business all'interno di una {@link Pipeline}.
 *
 * <p>Riceve il contesto dell'esecuzione, può leggerlo e modificarlo: il contesto è il canale con
 * cui uno step passa risultati agli step successivi. Uno step che deve fallire per una regola di
 * business lancia {@link BusinessRuleException} (o una sua sottoclasse).
 *
 * @param <C> tipo del contesto
 */
@FunctionalInterface
public interface Step<C> {

    /**
     * Esegue la logica dello step.
     *
     * @param context contesto dell'esecuzione corrente, mai null
     */
    void execute(C context);
}
