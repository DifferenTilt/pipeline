# pipeline-spring-boot-starter

Spring Boot 4.1 starter for `pipeline-core`. Adding it gives an application:

1. a `StepActivation` bean that disables the steps listed in `pipeline.disabled-steps`;
2. a `PipelineFactory` bean already wired to the activation, the global listeners, the listener
   error policy and the `Clock` bean, if there is one;
3. a startup check: if the configuration mentions pipelines or steps that do not exist, the
   application does not start;
4. in servlet web applications, a `@RestControllerAdvice` that translates pipeline exceptions into
   `ProblemDetail` responses (RFC 9457).

Every component can be replaced by defining a bean of the same type, or switched off with a
property.

## Dependency

```xml
<dependency>
    <groupId>io.github.differentilt</groupId>
    <artifactId>pipeline-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Configuration

```yaml
pipeline:
  disabled-steps:
    - create-product.date-range
  logging:
    enabled: true
```

| Property | Default | Description |
|---|---|---|
| `pipeline.disabled-steps` | empty | Disabled steps, in the form `<pipeline>.<step>` |
| `pipeline.fail-on-unknown-steps` | `true` | If `true`, entries that do not match an existing pipeline or step prevent startup; if `false` they are only logged at `WARN` |
| `pipeline.listener-error-policy` | `LOG_AND_CONTINUE` | Policy passed to the `PipelineFactory` (`LOG_AND_CONTINUE` or `PROPAGATE`) |
| `pipeline.logging.enabled` | `false` | Registers `LoggingPipelineListener` as a global listener |
| `pipeline.web.exception-handler.enabled` | `true` | Enables the default HTTP handler |
| `pipeline.web.include-location` | `false` | Adds `pipeline` and `step` to business error responses |
| `pipeline.web.status-mapping` | empty | Overrides the category → HTTP status mapping (values 400–599) |

### Format of `pipeline.disabled-steps`

Each entry has the form `<pipeline-name>.<step-name>`, with kebab-case names. Leading and trailing
whitespace is removed. A malformed entry always prevents startup with a
`PipelineConfigurationException`, even with `fail-on-unknown-steps=false`.

A step declared with `requiredStep` cannot be disabled: an entry that refers to it also always
prevents startup.

The list can also be set from an environment variable, which is handy for a quick rollback in
containers:

```
PIPELINE_DISABLEDSTEPS=create-product.date-range,update-product.audit
```

For toggles that change at runtime (Togglz, OpenFeature, a database), define your own
`StepActivation` bean: the default one is then not created.

## Composing a pipeline

```java
@Configuration
class CreateProductPipelineConfig {

    @Bean
    Pipeline<CreateProductContext> createProductPipeline(PipelineFactory factory,
            NameUniqueStep nameUnique, DateRangeStep dateRange, SaveProductStep save) {
        return factory.<CreateProductContext>pipeline("create-product")
                .step("name-unique", nameUnique)
                .step("date-range", dateRange)
                .requiredStep("save-product", save)
                .build();
    }
}
```

`requiredStep` marks a step that the configuration must not be able to switch off: it is always
executed, whatever `pipeline.disabled-steps` or a custom `StepActivation` says.

## Using it in a service

The transaction remains the caller's responsibility: an exception propagated by the pipeline rolls
back every step that has already run.

```java
@Service
class ProductService {

    private final Pipeline<CreateProductContext> createProduct;

    ProductService(Pipeline<CreateProductContext> createProduct) {
        this.createProduct = createProduct;
    }

    @Transactional
    public Product create(CreateProductRequest request) {
        return createProduct.run(new CreateProductContext(request)).getSavedProduct();
    }
}
```

**Unique constraints in the database.** For duplicates, the guarantee must come from a `UNIQUE`
constraint on the table. The save step catches the `DataIntegrityViolationException` and turns it
into a `BusinessRuleException` with category `CONFLICT`: it is the step that knows which constraint
was violated, not the HTTP handler.

## Listeners

All beans of type `PipelineListener<Object>` are global listeners: they are registered on the
`PipelineFactory` in the order given by `@Order` or `Ordered`, and notified for every pipeline
created by the factory.

**Limitation: context-specific listeners.** A bean declared as
`PipelineListener<CreateProductContext>` is not a global listener and is **not** registered
automatically, because its generic type does not match `PipelineListener<Object>`. It must be added
explicitly on the builder of the pipeline it belongs to:

```java
factory.<CreateProductContext>pipeline("create-product")
        .listener(createProductListener)
        // ...
        .build();
```

## Startup validation

Once all singletons have been created, `PipelineDefinitionsValidator` checks that:

- no two `Pipeline` beans have the same name (always fatal: toggles would be ambiguous);
- no entry of `pipeline.disabled-steps` refers to a required step (always fatal: the step would
  run anyway, while the configuration says it is disabled);
- every entry of `pipeline.disabled-steps` refers to an existing pipeline and step.

All problems are collected and reported in a single `PipelineConfigurationException`, one per line.

**Limitation: only pipelines registered as beans are validated.** A pipeline built inside a
constructor or a method is not visible to the validator, and its toggles would be reported as
"unknown". The recommendation is to always declare pipelines as beans. Required steps of such a
pipeline are still always executed, because that is enforced by `Pipeline` itself, but an entry
that tries to disable them is not reported at startup.

## HTTP error handling

In servlet web applications `PipelineExceptionHandler` produces `application/problem+json`
responses.

`BusinessRuleException`:

| Category | Status |
|---|---|
| `INVALID_INPUT` | 400 |
| `FORBIDDEN` | 403 |
| `NOT_FOUND` | 404 |
| `CONFLICT` | 409 |

The body contains `title`, `status`, `detail` (the exception message), `code`, `details` when not
empty and, with `pipeline.web.include-location=true`, `pipeline` and `step`. The mapping can be
overridden entry by entry, for example `pipeline.web.status-mapping.INVALID_INPUT=422`. These errors
are not logged by the handler.

`StepExecutionException`: 500 with `detail` `An internal error occurred.` and `code`
`INTERNAL_ERROR`. The message, cause, pipeline and step are never exposed to the client; the
exception is logged at `ERROR` with its stack trace.

### Order relative to the project's own advice

The handler has order `LOWEST_PRECEDENCE - 100`. A project advice without `@Order` (which means
`LOWEST_PRECEDENCE`) with a catch-all `@ExceptionHandler(Exception.class)` therefore does **not**
intercept pipeline exceptions before the starter's handler. To handle them in your own code there
are three options:

1. extend `PipelineExceptionHandler` (recommended);
2. disable it with `pipeline.web.exception-handler.enabled=false`;
3. give your advice an `@Order` with higher precedence.

### Customising the handler

A subclass registered as a bean replaces the default handler.

```java
@Component
class MyPipelineExceptionHandler extends PipelineExceptionHandler {

    MyPipelineExceptionHandler(PipelineWebProperties properties) {
        super(properties);
    }

    @Override
    protected ProblemDetail businessProblem(BusinessRuleException e) {
        ProblemDetail pd = super.businessProblem(e);
        pd.setType(URI.create("https://errors.example.com/" + e.getCode()));
        return pd;
    }
}
```
