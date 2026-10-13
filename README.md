# pipeline

A small Java library for running business logic as a sequence of named, configurable steps that
share a context, plus a Spring Boot starter that wires it into an application.

```java
Pipeline<CreateProductContext> createProduct = Pipeline.<CreateProductContext>builder("create-product")
        .step("name-unique", new NameUniqueStep(repository))
        .step("date-range", new DateRangeStep())
        .step("save-product", new SaveProductStep(repository))
        .build();

Product product = createProduct.run(new CreateProductContext(request)).getSavedProduct();
```

- Steps run in order and share a typed context; the first one that throws stops the pipeline.
- Each step can be switched off by configuration, without touching the code.
- Listeners receive lifecycle events for logging, metrics and auditing.
- Business errors carry a stable code and a category; unexpected errors are wrapped with the
  pipeline and step in which they occurred.

## Modules

| Module | Description |
|---|---|
| [`pipeline-core`](pipeline-core/README.md) | The library itself. Java 17, no runtime dependencies. |
| [`pipeline-spring-boot-starter`](pipeline-spring-boot-starter/README.md) | Spring Boot 4.1 starter: step toggles from `pipeline.disabled-steps`, a preconfigured `PipelineFactory`, startup validation of the configuration, and `ProblemDetail` responses for pipeline exceptions. |

## Getting started

The artifacts are not published to a public repository yet. Build and install them locally:

```
mvn install
```

Then add the module you need to your project:

```xml
<!-- plain Java -->
<dependency>
    <groupId>io.github.differentilt</groupId>
    <artifactId>pipeline-core</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>

<!-- Spring Boot (brings pipeline-core with it) -->
<dependency>
    <groupId>io.github.differentilt</groupId>
    <artifactId>pipeline-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

See the module READMEs for usage and configuration.

## Building

Requires JDK 17 or later and Maven 3.9.

```
mvn verify
```

The build compiles with `-Xlint:all` and fails on compiler warnings.

## Releasing

Releases are cut by the **Release** workflow, run by hand from the Actions tab. It takes the kind of
release and computes the version from the latest `vX.Y.Z` tag:

| Kind | Run it on | From 2.7.5 |
|---|---|---|
| `minor` | `develop` | 2.8.0 |
| `major` | `develop` | 3.0.0 |
| `hotfix` | a branch created from `main` that holds the fix | 2.7.6 |

The workflow creates `release/X.Y.Z`, sets the version in the POMs and publishes to Maven Central
with the `release` profile. Only if the publication succeeds, it merges the branch into `main`, tags
it `vX.Y.Z`, deletes it, merges `main` back into `develop` and moves `develop` to the next
`-SNAPSHOT`. If the publication fails the release branch is deleted and nothing else changes.

Ticking **Dry run** builds the release without signing it, and pushes and publishes nothing.

The workflow needs four repository secrets: `CENTRAL_USERNAME` and `CENTRAL_PASSWORD` (a Central
Portal user token), `GPG_PRIVATE_KEY` (the armored private key) and `GPG_PASSPHRASE`.

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
