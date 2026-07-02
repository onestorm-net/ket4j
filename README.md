# ket4j

Java port of [kendo-error-tracker](https://github.com/script-development/kendo-error-tracker). Sends sanitized exception reports to kendo's ingestion API from Java applications via Log4j2.

## Modules

| Module | Description |
|---|---|
| `ket4j-core` | Config, sanitizers, HTTP client — no external dependencies |
| `ket4j-log4j2` | Log4j2 appender |

## Running tests

```bash
mvn verify
```

Tests and JaCoCo 100% line coverage check run together. To run a single module:

```bash
mvn verify -pl ket4j-core
```

## Usage

**1. Add the dependency**

```xml
<dependency>
    <groupId>net.onestorm.ket4j</groupId>
    <artifactId>ket4j-log4j2</artifactId>
    <version>1.0.0</version>
</dependency>
```

**2. Initialize the provider at application startup**

```java
ErrorTrackerProvider.initialize(
    ErrorTrackerConfiguration.builder()
        .kendoUrl("https://kendo.example.com")
        .projectId("your-project-id")
        .token("your-token")
        .environment("production")   // default
        .release("1.2.3")            // optional
        .build()
);
```

**3. Add the appender to `log4j2.xml`**

```xml
<Appenders>
    <KendoError name="Kendo"/>
</Appenders>

<Loggers>
    <Root level="warn">
        <AppenderRef ref="Kendo"/>
    </Root>
</Loggers>
```

The appender forwards `WARN`, `ERROR`, and `FATAL` events to kendo. Sensitive data (JWTs, bearer tokens, DSN passwords, Stripe/AWS keys, IPv4 addresses, emails, Dutch BSNs, and file paths) is redacted before sending.
