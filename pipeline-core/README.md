# pipeline-core

A Java 17 library with no runtime dependencies for running business logic as a sequence of
configurable steps that share a context.

- **Pipeline**: an ordered, immutable sequence of named steps, executed with `run(context)`.
- **Step**: a unit of business logic; it reads and modifies the context.
- **Context**: a typed, user-defined object, one per execution.
- **Activation** (`StepActivation`): decides on every execution whether a step is enabled; disabled
  steps are skipped.
- **Listener** (`PipelineListener`): receives lifecycle events (start, step, completion, failure)
  for logging, metrics and auditing.
- **Fail-fast**: the first step that throws stops the pipeline, and the exception is propagated to
  the caller.

Pipeline and step names are kebab-case (`^[a-z0-9]+(-[a-z0-9]+)*$`).

## Packages

| Package | Contents |
|---|---|
| `com.dtilt.pipeline` | `Pipeline`, `PipelineFactory` |
| `com.dtilt.pipeline.step` | `Step`, `StepActivation` |
| `com.dtilt.pipeline.exception` | `PipelineException`, `BusinessRuleException`, `StepExecutionException` |
| `com.dtilt.pipeline.event` | `PipelineEvent` and the event records (`PipelineStarted`, `StepCompleted`, ...) |
| `com.dtilt.pipeline.listener` | `PipelineListener`, `ListenerErrorPolicy`, `LoggingPipelineListener` |

## Usage example

```java
public class CreateProductContext {
    private final CreateProductRequest request;
    private Product savedProduct;
    // constructor, getters, setters
}

class NameUniqueStep implements Step<CreateProductContext> {
    private final ProductRepository repository;

    @Override
    public void execute(CreateProductContext ctx) {
        String name = ctx.getRequest().name();
        if (repository.existsByName(name)) {
            throw new BusinessRuleException(
                "PRODUCT_NAME_DUPLICATE",
                BusinessRuleException.Category.CONFLICT,
                "A product named '" + name + "' already exists",
                Map.of("name", name));
        }
    }
}

PipelineFactory factory = PipelineFactory.builder()
        .activation(StepActivation.disabling(Map.of("create-product", Set.of("date-range"))))
        .listener(new LoggingPipelineListener())
        .build();

Pipeline<CreateProductContext> createProduct = factory.<CreateProductContext>pipeline("create-product")
        .step("name-unique", new NameUniqueStep(repository))
        .step("date-range", new DateRangeStep())
        .step("save-product", new SaveProductStep(repository))
        .build();

try {
    Product product = createProduct.run(new CreateProductContext(request)).getSavedProduct();
} catch (BusinessRuleException e) {
    // expected error: e.getCode(), e.getCategory(), e.getStep()
} catch (StepExecutionException e) {
    // technical error: e.getCause()
}
```

## Exceptions

| Thrown by the step | Propagated by `run` |
|---|---|
| `PipelineException` (e.g. `BusinessRuleException`) | the same instance, with `getPipeline()` and `getStep()` set |
| any other `RuntimeException` | `StepExecutionException` with the original as its cause |
| `Error` | unchanged, without failure events |

With nested pipelines the innermost location is kept.

## Listeners

`Pipeline` only calls `PipelineListener.onEvent`; the default implementation dispatches to the
specific methods (`onStepStarted`, `onPipelineFailed`, ...). Listeners must not modify the context.

With `ListenerErrorPolicy.LOG_AND_CONTINUE` (the default) a listener that throws is logged at
`WARNING` and does not change the outcome of the pipeline; with `PROPAGATE` the exception reaches
the caller (useful in tests).

`LoggingPipelineListener` is an optional listener based on `System.Logger` (logger name
`com.dtilt.pipeline`); it is not registered automatically.

## Build

```
mvn verify
```
