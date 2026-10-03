# Decisioni

Decisioni prese su punti ambigui di `pipeline-core-spec.md`.

## Progetto

- **groupId `com.dtilt.pipeline`.** Scelto al posto del segnaposto `com.example` della specifica
  originale, per coerenza con il package base `com.dtilt.pipeline`. Le coordinate del modulo sono
  `com.dtilt.pipeline:pipeline-core`.
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
