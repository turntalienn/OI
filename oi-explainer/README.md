# OI-Explainer (`oi-explainer`)

`oi-explainer` is a Rust-based microservice that ingests `FlowTree` JSON data from the `oi-core` agent, processes it in real-time, and returns a human-readable explanation of the execution flow.

## Features

- **Real-Time Ingestion**: A non-blocking Actix-Web server provides a high-performance endpoint for receiving trace data.
- **Strict Schema Validation**: Ingests JSON that strictly matches the `FlowTree` schema, rejecting malformed requests.
- **Narrative Generation**: Transforms the raw, nested call tree into a flattened, step-by-step narrative that is easy for developers to understand.
- **Configurable**: Key parameters can be configured via a `config.toml` file.

---

## API Specification

### `POST /ingest`

Accepts a `FlowTree` JSON object and returns a processed explanation.

**Request Body:**

The body must be a JSON object matching the schema produced by `oi-core`. See the `oi-core` data model for details.

**Success Response (200 OK):**

```json
{
  "summary": "The request was handled by the entry point `greet` in class `com.mycompany.service.GreetingService`. It took 15 ms and completed successfully.",
  "steps": [
    {
      "step": 1,
      "description": "Call to `greet` on thread `virtual-thread-1` took 15 ms."
    },
    {
      "step": 2,
      "description": "Call to `formatGreeting` on thread `virtual-thread-1` took 5 ms."
    }
  ],
  "spiralSvg": "<svg>...</svg>",
  "branchProbabilities": []
}
```

**Headers:**

- `Accept: application/pdf`: If this header is present, the service will return a PDF document instead of JSON. (Note: PDF generation is not yet implemented).

---

## Configuration

The service is configured via a `config.toml` file in the same directory as the executable.

```toml
# config.toml

[filter]
# A list of package prefixes to exclude from the final output.
exclude_packages = ["org.springframework", "java.lang"]

[visual]
# Parameters for controlling the (not yet implemented) spiral visualization.
depth_scale = 10.0
angle_scale = 0.001
```

---

## How to Run

1.  **Build the Service:**
    ```bash
    cargo build --release
    ```

2.  **Run the Executable:**
    ```bash
    ./target/release/oi-explainer
    ```

The server will start on `http://127.0.0.1:8081`. 