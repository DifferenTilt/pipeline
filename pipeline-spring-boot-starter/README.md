# pipeline-spring-boot-starter

Starter Spring Boot 4.1 per `pipeline-core`. Aggiungendolo, un'applicazione ottiene:

1. un bean `StepActivation` che disattiva gli step elencati in `pipeline.disabled-steps`;
2. un bean `PipelineFactory` già collegato all'attivazione, ai listener globali, alla policy di
   errore dei listener e al bean `Clock`, se presente;
3. un controllo all'avvio: se la configurazione cita pipeline o step inesistenti, l'applicazione
   non parte;
4. nelle applicazioni web servlet, un `@RestControllerAdvice` che traduce le eccezioni della
   pipeline in risposte `ProblemDetail` (RFC 9457).

Ogni componente è sostituibile definendo un proprio bean dello stesso tipo, o disattivabile da
property.

## Dipendenza

```xml
<dependency>
    <groupId>com.dtilt.pipeline</groupId>
    <artifactId>pipeline-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Configurazione

```yaml
pipeline:
  disabled-steps:
    - create-product.date-range
  logging:
    enabled: true
```

| Property | Default | Descrizione |
|---|---|---|
| `pipeline.disabled-steps` | vuota | Step disattivati, nel formato `<pipeline>.<step>` |
| `pipeline.fail-on-unknown-steps` | `true` | Se `true`, le voci che non corrispondono a pipeline o step esistenti bloccano l'avvio; se `false` vengono solo loggate come `WARN` |
| `pipeline.listener-error-policy` | `LOG_AND_CONTINUE` | Policy passata alla `PipelineFactory` (`LOG_AND_CONTINUE` o `PROPAGATE`) |
| `pipeline.logging.enabled` | `false` | Registra `LoggingPipelineListener` come listener globale |
| `pipeline.web.exception-handler.enabled` | `true` | Attiva l'handler HTTP di default |
| `pipeline.web.include-location` | `false` | Aggiunge `pipeline` e `step` alle risposte di errore di business |
| `pipeline.web.status-mapping` | vuota | Sovrascrive la mappatura categoria → status HTTP (valori 400–599) |

### Formato di `pipeline.disabled-steps`

Ogni voce ha la forma `<nome-pipeline>.<nome-step>`, con nomi in kebab-case. Gli spazi iniziali e
finali vengono rimossi. Una voce malformata blocca sempre l'avvio con
`PipelineConfigurationException`, anche con `fail-on-unknown-steps=false`.

La lista si imposta anche da variabile d'ambiente, utile per un rollback rapido in container:

```
PIPELINE_DISABLEDSTEPS=create-product.date-range,update-product.audit
```

Per toggle modificabili a runtime (Togglz, OpenFeature, un database) basta definire un proprio bean
`StepActivation`: quello di default non viene creato.

## Composizione della pipeline

```java
@Configuration
class CreateProductPipelineConfig {

    @Bean
    Pipeline<CreateProductContext> createProductPipeline(PipelineFactory factory,
            NameUniqueStep nameUnique, DateRangeStep dateRange, SaveProductStep save) {
        return factory.<CreateProductContext>pipeline("create-product")
                .step("name-unique", nameUnique)
                .step("date-range", dateRange)
                .step("save-product", save)
                .build();
    }
}
```

## Uso nel service

La transazione resta responsabilità del chiamante: un'eccezione propagata dalla pipeline provoca
il rollback di tutti gli step già eseguiti.

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

**Vincoli univoci a DB.** Per i duplicati la garanzia va data da un vincolo `UNIQUE` sulla tabella.
Lo step di salvataggio intercetta la `DataIntegrityViolationException` e la trasforma in una
`BusinessRuleException` con categoria `CONFLICT`: è lo step a sapere quale vincolo è stato violato,
non l'handler HTTP.

## Listener

Tutti i bean di tipo `PipelineListener<Object>` sono listener globali: vengono registrati sulla
`PipelineFactory` nell'ordine dato da `@Order` o `Ordered` e notificati per ogni pipeline creata
dalla factory.

**Limite: listener specifici di un contesto.** Un bean dichiarato come
`PipelineListener<CreateProductContext>` non è un listener globale e **non** viene registrato
automaticamente, perché il tipo generico non corrisponde a `PipelineListener<Object>`. Va aggiunto
esplicitamente sul builder della pipeline a cui si riferisce:

```java
factory.<CreateProductContext>pipeline("create-product")
        .listener(createProductListener)
        // ...
        .build();
```

## Validazione all'avvio

Dopo la creazione di tutti i singleton, `PipelineDefinitionsValidator` controlla che:

- due bean `Pipeline` non abbiano lo stesso nome (sempre bloccante: i toggle sarebbero ambigui);
- ogni voce di `pipeline.disabled-steps` si riferisca a una pipeline e a uno step esistenti.

Tutti i problemi vengono raccolti e riportati in un'unica `PipelineConfigurationException`, uno
per riga.

**Limite: sono validate solo le pipeline registrate come bean.** Una pipeline costruita dentro un
costruttore o un metodo non è visibile al validatore, e i suoi toggle risulterebbero "sconosciuti".
La raccomandazione è dichiarare sempre le pipeline come bean.

## Gestione degli errori HTTP

Nelle applicazioni web servlet `PipelineExceptionHandler` produce risposte `application/problem+json`.

`BusinessRuleException`:

| Categoria | Status |
|---|---|
| `INVALID_INPUT` | 400 |
| `FORBIDDEN` | 403 |
| `NOT_FOUND` | 404 |
| `CONFLICT` | 409 |

Il body contiene `title`, `status`, `detail` (il messaggio dell'eccezione), `code`, `details` se non
vuota e, con `pipeline.web.include-location=true`, `pipeline` e `step`. La mappatura si sovrascrive
voce per voce, ad esempio `pipeline.web.status-mapping.INVALID_INPUT=422`. Questi errori non vengono
loggati dall'handler.

`StepExecutionException`: 500 con `detail` `An internal error occurred.` e `code` `INTERNAL_ERROR`.
Messaggio, causa, pipeline e step non vengono mai esposti al client; l'eccezione viene loggata a
livello `ERROR` con lo stack trace.

### Ordine rispetto agli advice del progetto

L'handler ha ordine `LOWEST_PRECEDENCE - 100`. Un advice del progetto senza `@Order` (che vale
`LOWEST_PRECEDENCE`) con un catch-all `@ExceptionHandler(Exception.class)` quindi **non** intercetta
le eccezioni della pipeline prima dell'handler dello starter. Per gestirle nel proprio codice ci
sono tre strade:

1. estendere `PipelineExceptionHandler` (raccomandato);
2. disattivarlo con `pipeline.web.exception-handler.enabled=false`;
3. dare al proprio advice un `@Order` con precedenza più alta.

### Personalizzazione dell'handler

La sottoclasse registrata come bean sostituisce l'handler di default.

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
