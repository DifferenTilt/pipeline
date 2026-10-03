# pipeline-core

Libreria Java 17 senza dipendenze runtime per eseguire logica di business come una sequenza di
step configurabili che condividono un contesto.

- **Pipeline**: sequenza ordinata e immutabile di step con un nome, eseguita con `run(context)`.
- **Step**: un'unità di logica di business; legge e modifica il contesto.
- **Contesto**: oggetto tipizzato definito dall'utente, uno per esecuzione.
- **Attivazione** (`StepActivation`): decide a ogni esecuzione se uno step è attivo; gli step
  disattivati vengono saltati.
- **Listener** (`PipelineListener`): riceve gli eventi del ciclo di vita (avvio, step,
  completamento, fallimento) per logging, metriche, audit.
- **Fail-fast**: il primo step che lancia un'eccezione interrompe la pipeline e l'eccezione viene
  propagata al chiamante.

Nomi di pipeline e step sono in kebab-case (`^[a-z0-9]+(-[a-z0-9]+)*$`).

## Esempio d'uso

```java
public class CreateProductContext {
    private final CreateProductRequest request;
    private Product savedProduct;
    // costruttore, getter, setter
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
    // errore atteso: e.getCode(), e.getCategory(), e.getStep()
} catch (StepExecutionException e) {
    // errore tecnico: e.getCause()
}
```

## Eccezioni

| Lanciata dallo step | Propagata da `run` |
|---|---|
| `PipelineException` (es. `BusinessRuleException`) | la stessa istanza, con `getPipeline()` e `getStep()` valorizzati |
| altra `RuntimeException` | `StepExecutionException` con l'originale come causa |
| `Error` | invariato, senza eventi di fallimento |

Con pipeline annidate viene mantenuta la posizione più interna.

## Listener

`Pipeline` chiama solo `PipelineListener.onEvent`; il default smista verso i metodi specifici
(`onStepStarted`, `onPipelineFailed`, ...). I listener non devono modificare il contesto.

Con `ListenerErrorPolicy.LOG_AND_CONTINUE` (default) un listener che lancia un'eccezione viene
loggato a `WARNING` e non cambia l'esito della pipeline; con `PROPAGATE` l'eccezione raggiunge il
chiamante (utile nei test).

`LoggingPipelineListener` è un listener opzionale basato su `System.Logger` (logger
`com.dtilt.pipeline`); non viene registrato automaticamente.

## Build

```
mvn verify
```
