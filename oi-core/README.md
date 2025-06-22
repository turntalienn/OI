# OI-Core (`oi-core`)

`oi-core` is a production-ready, in-memory observability library for Java 21 applications. It uses bytecode weaving at runtime to capture detailed performance and execution data for specific methods, constructs an in-memory call tree for each request, and emits the data to a configurable endpoint without ever writing to disk.

## Features

- **In-Memory Tracing**: All trace data is collected in a `ThreadLocal` context and held in memory for the duration of a request. No persistent storage, no file I/O.
- **Bytecode Weaving**: Uses the high-performance ASM library to instrument methods at runtime.
- **Rich Data Collection**:
    - FQDN class + method names.
    - Method parameter names and values.
    - Return values and exceptions.
    - Nanosecond-precision start and end timestamps.
    - Thread ID and type (platform vs. virtual).
    - Bytecode instruction count and local variable usage.
    - **[Experimental]** AST-based analysis of conditional branches.
- **Configurable Emitter**: Ships with a non-blocking HTTP emitter to send trace data as JSON to a remote ingestion service.
- **Lightweight & High-Performance**: Designed for minimal overhead, using virtual threads for background tasks and caching for reflection.

---

## Quickstart

To use `oi-core`, you need to attach it as a Java Agent to your application's startup command.

### 1. Build the Agent

Build the project using Maven or Gradle:

**Maven:**
```bash
mvn clean package
```

**Gradle:**
```bash
./gradlew build
```
This will produce `oi-core-1.0.0-SNAPSHOT.jar` in the `target` (Maven) or `build/libs` (Gradle) directory.

### 2. Attach the Agent

Add the `-javaagent` flag to your application's startup command, pointing to the location of the `oi-core` JAR.

```bash
java -javaagent:/path/to/oi-core-1.0.0-SNAPSHOT.jar -jar my-application.jar
```

The agent will now be active and will start instrumenting classes based on the default configuration.

---

## Configuration

`oi-core` is configured via a set of properties. In a standalone environment, these can be provided as system properties (`-Doi-core.enabled=true`). When used with the `oi-spring-adapter`, they can be defined in `application.yml` or `application.properties`.

| Property                             | Description                                                                                             | Default                                    |
|--------------------------------------|---------------------------------------------------------------------------------------------------------|--------------------------------------------|
| `oi-core.enabled`                    | Master switch to enable or disable the agent.                                                           | `true`                                     |
| `oi-core.includePackages`            | A list of package prefixes to instrument.                                                               | `[ "com.mycompany" ]`                        |
| `oi-core.excludeAnnotations`         | A list of FQDN annotation names to exclude from instrumentation.                                        | `[ "o.s.stereotype.Component" ]`           |
| `oi-core.emitter.url`                | The HTTP URL of the ingestion service.                                                                  | `http://localhost:8081/ingest`             |
| `oi-core.emitter.timeoutMs`          | Timeout in milliseconds for the HTTP emitter.                                                           | `200`                                      |
| `oi-core.instrumentation.controller` | Instrument methods in classes annotated with `@Controller` or `@RestController`.                        | `true`                                     |
| `oi-core.instrumentation.service`    | Instrument methods in classes annotated with `@Service`.                                                | `true`                                     |
| `oi-core.instrumentation.repository` | Instrument methods in classes annotated with `@Repository`.                                             | `true`                                     |
| `oi-core.instrumentation.threads`    | Instrument `java.lang.Thread.start()` to trace new thread creation.                                     | `true`                                     |
| `oi-core.instrumentation.ast`        | **(Experimental)** Enable Abstract Syntax Tree analysis for conditional branches. Requires source on classpath. | `true`                                     |
| `oi-core.instrumentation.bytecode`   | Enable collection of bytecode-level metrics (instruction count, locals).                                | `true`                                     |

---

## FAQ

### Q: Why is there no persistent storage?

**A:** This library is designed for real-time, in-flight observability where the goal is to immediately process and analyze execution flows. By avoiding disk I/O entirely, `oi-core` minimizes performance overhead and simplifies the agent's design. It assumes that a remote service (like `oi-explainer`) is responsible for any long-term storage or analysis if needed.

### Q: The "conditional branches" are not showing up. Why?

**A:** The AST (Abstract Syntax Tree) analysis feature, which finds `if` and `switch` statements, requires the Java source code (`.java` files) of the instrumented classes to be available on the application's classpath at runtime. This is not a typical configuration for production environments. If the source files are not found, this feature will be silently skipped. For reliable analysis, consider a build-time transformation approach.

### Q: What is the performance overhead?

**A:** The overhead is designed to be minimal (sub-5% in most HelloWorld-style applications). The most expensive operations are:
1.  **AST Parsing**: If enabled and source is available, this adds parsing overhead.
2.  **Reflection for Parameter Names**: This is heavily cached after the first lookup per method.
3.  **JSON Serialization**: This is done off the request thread by a virtual thread.

For performance-critical applications, you can disable the `ast` and `bytecode` instrumentation flags to reduce overhead further. Benchmarks will be provided in a future release. 