# Decisioni

Decisioni prese su punti ambigui di `pipeline-core-spec.md` e
`pipeline-spring-boot-starter-spec.md`.

# pipeline-core

## Progetto

- **groupId `com.dtilt.pipeline`.** Scelto al posto del segnaposto `com.example` della specifica
  originale, per coerenza con il package base `com.dtilt.pipeline`. Le coordinate del modulo sono
  `com.dtilt.pipeline:pipeline-core`.
- **Layout dei package.** Su richiesta dell'utente il core è suddiviso in `com.dtilt.pipeline`
  (`Pipeline`, `PipelineFactory`, `Step`, `StepActivation`, `ListenerChain`),
  `com.dtilt.pipeline.exception` (le tre eccezioni), `com.dtilt.pipeline.event` (`PipelineEvent` e i
  record) e `com.dtilt.pipeline.listener` (`PipelineListener`, `ListenerErrorPolicy`,
  `LoggingPipelineListener`), invece dei due soli package della specifica originale.
- **`PipelineException.locate` è `public final`.** Con le eccezioni in un package diverso da
  `Pipeline` non può più essere package-private. Resta innocuo: assegna la posizione solo se non
  è già assegnata.
- **Radice del progetto = `pipeline-parent`.** Il `pom.xml` del parent sta nella directory radice
  `pipeline/`, che contiene anche le specifiche; i moduli stanno nelle sottodirectory
  (`pipeline-core/` e, in futuro, `pipeline-spring-boot-starter/`).
- **Versioni.** JUnit Jupiter 5.14.4 (ultima 5.x: la specifica chiede JUnit 5, non 6), AssertJ
  3.27.7, maven-compiler-plugin 3.15.0, maven-surefire-plugin 3.5.6.
- **Warning di compilazione.** Il compilatore gira con `-Xlint:all` e `failOnWarning`, così il
  requisito "nessun warning" di §12 è verificato dalla build. Due `@SuppressWarnings` mirati:
  `serial` sul campo `details` di `BusinessRuleException` e `this-escape` sul costruttore di
  `StepExecutionException` (che per specifica chiama `locate`).
- **Lingua.** Javadoc in italiano come la specifica; messaggi di eccezioni e log in inglese, come
  il formato del messaggio di `StepExecutionException` in §5.4.

## Validazione

- **Nomi null o non kebab-case.** Nome null → `NullPointerException`; nome blank o non kebab-case
  → `IllegalArgumentException`. Il nome della pipeline è validato in `Pipeline.builder(name)`.
- **`BusinessRuleException`.** `code`, `category`, `message` e `details` null →
  `NullPointerException`; `code` blank → `IllegalArgumentException`. `details` viene copiata in
  una mappa non modificabile che conserva l'ordine e ammette valori null.
- **`StepExecutionException`.** `pipeline`, `step` e `cause` null → `NullPointerException`.
- **`StepActivation.disabling`.** Mappa, chiavi e valori null → `NullPointerException`.
- **`runIdSupplier` che restituisce null o lancia un'eccezione.** `run` lancia rispettivamente
  `NullPointerException` o l'eccezione del supplier, prima di emettere qualsiasi evento.

## Esecuzione ed eventi

- **`locate`** considera la posizione "già assegnata" quando pipeline o step sono valorizzati.
- **`occurredAt`** viene letto dal `Clock` una volta per evento.
- **Durate.** `StepCompleted`/`StepFailed` misurano la sola `step.execute`; la durata della
  pipeline parte subito dopo la generazione del `runId`. Sono forzate a un minimo di zero.
- **Eccezioni dei listener.** `ListenerChain` intercetta solo `RuntimeException`; un `Error` di un
  listener si propaga sempre. Con `PROPAGATE` l'eccezione del listener non viene trattata come
  fallimento di uno step: né tradotta né seguita da `StepFailed`/`PipelineFailed`. Se però la
  pipeline è annidata, la pipeline esterna la vede come una normale eccezione del proprio step e
  la incapsula in `StepExecutionException`.
- **Logger di `ListenerChain`.** Usa il logger `com.dtilt.pipeline`, lo stesso di
  `LoggingPipelineListener`.

## `PipelineFactory`

- Non espone `runIdSupplier` (non previsto dalla firma in §4.4): ogni pipeline usa il default o
  quello impostato sul proprio builder.
- I listener globali sono `PipelineListener<Object>`, come da firma.

## `LoggingPipelineListener`

- **Sottotipi di `PipelineException` diversi dai due previsti.** `PipelineFailed` con un errore
  che non è una `BusinessRuleException` viene loggato a `WARNING`, come un errore tecnico.
- **Costruttore package-private con `System.Logger`**, usato solo dai test per verificare livelli
  e contenuto dei messaggi. Il nome del logger è esposto come costante `LOGGER_NAME`.
- Le durate sono loggate in millisecondi.

# pipeline-spring-boot-starter

## Build e dipendenze

- **Spring Boot 4.1.1**, ultima patch 4.1.x disponibile; BOM importato nel parent.
- **JUnit 6 solo nello starter.** `spring-boot-test` 4.1 richiede le API di JUnit 6 (con JUnit 5
  `OutputCaptureExtension` fallisce con `NoSuchMethodError`). Il parent continua a gestire JUnit
  5.14.4 per `pipeline-core`; il `pom.xml` dello starter importa `junit-bom` 6.0.3 (la versione
  gestita da Spring Boot 4.1.1) nel proprio `dependencyManagement`. `pipeline-core` non è toccato.
- **Jackson in scope `test`.** `tools.jackson.core:jackson-databind` è aggiunto come dipendenza di
  test: `spring-boot-starter-test` non lo include e serve a MockMvc per serializzare `ProblemDetail`.
- **`-Xlint:all,-processing` nello starter.** Il configuration processor non reclama le annotazioni
  Spring e il lint `processing` lo segnalerebbe come warning, facendo fallire la build.
- **Configuration processor** dichiarato come dipendenza `optional`, come da tabella della
  specifica; il compilatore lo trova sul classpath.
- **Logging con `org.apache.commons.logging`.** SLF4J non è una dipendenza dello starter (arriva
  solo con `spring-boot-starter-logging` dell'applicazione), quindi validatore e handler usano
  Commons Logging di spring-core, come previsto dalla specifica.

## Configurazione

- **Properties come classi JavaBean**, non record: i commenti Javadoc sui campi sono la forma più
  diretta per generare le descrizioni nei metadati.
- **`pipeline.logging.enabled` e `pipeline.web.exception-handler.enabled`** sono anche campi di
  `PipelineProperties` e `PipelineWebProperties` (classi annidate), così compaiono nei metadati.
- **Voci malformate di `disabled-steps`.** Il parser le raccoglie tutte e le riporta in un'unica
  `PipelineConfigurationException`. Una voce null equivale a una voce vuota (malformata).
- **Validazione del formato "sempre attiva".** Il formato è validato sia alla creazione dello
  `StepActivation` di default sia da `PipelineDefinitionsValidator`, quindi anche quando il progetto
  definisce un proprio `StepActivation`.
- **`listener-error-policy` null** equivale al default `LOG_AND_CONTINUE`.

## Validazione all'avvio

- **Step di pipeline con nome duplicato.** Una voce è considerata nota se lo step esiste in almeno
  una delle pipeline con quel nome; il nome duplicato è comunque segnalato come errore.
- **Messaggio.** Prima riga `Invalid pipeline configuration:`, poi un problema per riga (prima i
  duplicati, poi le voci sconosciute).
- **`getBeansOfType(Pipeline.class)`** inizializza anche eventuali bean `Pipeline` lazy, come
  conseguenza dell'API indicata dalla specifica.

## Web

- **Status fuori da 400-599.** Validato nel costruttore di `PipelineExceptionHandler`, così vale
  anche per le sottoclassi del progetto. L'avvio fallisce con una `BeanCreationException` la cui
  causa è `PipelineConfigurationException`.
- **`title`.** Reason phrase dello status; per uno status configurato che non è uno `HttpStatus`
  noto (es. 499) il titolo non viene impostato.
- **`include-location` con eccezione non localizzata.** `pipeline` e `step` vengono omessi se null
  (eccezione lanciata fuori da una pipeline).
- **Log degli errori tecnici** in `handleStepExecution`, non in `technicalProblem`: una sottoclasse
  che sovrascrive `technicalProblem` mantiene il log.
- **Metodi `@Bean` package-private** in entrambe le auto-configuration, come nell'esempio di §7.1.

## Test

- **Test end-to-end** con `WebApplicationContextRunner` e MockMvc (entrambe le auto-configuration,
  `@EnableWebMvc`, controller, pipeline bean), come indicato in testa a §9, senza `@SpringBootTest`.
- **Advice catch-all di test** risponde 503 a un'eccezione non della pipeline, per verificare che
  sia effettivamente attivo.
- **Variabile d'ambiente `PIPELINE_DISABLEDSTEPS`** verificata con un
  `SystemEnvironmentPropertySource` di test.
