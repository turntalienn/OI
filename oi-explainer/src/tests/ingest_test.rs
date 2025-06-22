use actix_web::{test, App};
use oi_explainer::model::*;
use oi_explainer::ingest;
use serde_json::json;

#[actix_web::test]
async fn test_ingest_endpoint() {
    let app = test::init_service(App::new().service(ingest)).await;
    let flow_tree = json!({
        "traceId": "abc123",
        "rootNode": {
            "methodDetails": {
                "className": "com.mycompany.HelloController",
                "methodName": "hello",
                "methodSignature": "(Ljava/lang/String;)Ljava/lang/String;",
                "parameters": {"name": "World"}
            },
            "executionDetails": {
                "startNanos": 0,
                "endNanos": 1000000,
                "threadInfo": {
                    "threadId": 1,
                    "threadName": "main",
                    "isVirtual": false
                }
            },
            "codeAnalysis": {
                "instructionCount": 10,
                "maxLocalVariables": 2,
                "conditionalBranches": ["if(name != null)"]
            },
            "children": [],
            "dbEvents": [],
            "returnValue": "Hello, World!",
            "exception": null
        },
        "startNanos": 0,
        "endNanos": 1000000
    });
    let req = test::TestRequest::post()
        .uri("/ingest")
        .set_json(&flow_tree)
        .to_request();
    let resp = test::call_service(&app, req).await;
    assert!(resp.status().is_success());
} 