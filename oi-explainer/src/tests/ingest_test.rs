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
            "exception": null,
            "branchesTaken": ["if(name != null)"],
            "loopsEntered": []
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

#[actix_web::test]
async fn test_ingest_with_nested_calls() {
    let app = test::init_service(App::new().service(ingest)).await;
    let flow_tree = json!({
        "traceId": "nested123",
        "rootNode": {
            "methodDetails": {
                "className": "com.mycompany.Controller",
                "methodName": "process",
                "methodSignature": "()V",
                "parameters": {}
            },
            "executionDetails": {
                "startNanos": 0,
                "endNanos": 2000000,
                "threadInfo": {
                    "threadId": 1,
                    "threadName": "main",
                    "isVirtual": false
                }
            },
            "codeAnalysis": {
                "instructionCount": 15,
                "maxLocalVariables": 3,
                "conditionalBranches": ["if(condition)"]
            },
            "children": [
                {
                    "methodDetails": {
                        "className": "com.mycompany.Service",
                        "methodName": "validate",
                        "methodSignature": "(Ljava/lang/Object;)Z",
                        "parameters": {"input": "test"}
                    },
                    "executionDetails": {
                        "startNanos": 500000,
                        "endNanos": 1500000,
                        "threadInfo": {
                            "threadId": 1,
                            "threadName": "main",
                            "isVirtual": false
                        }
                    },
                    "codeAnalysis": {
                        "instructionCount": 8,
                        "maxLocalVariables": 2,
                        "conditionalBranches": []
                    },
                    "children": [],
                    "dbEvents": [],
                    "returnValue": true,
                    "exception": null,
                    "branchesTaken": [],
                    "loopsEntered": []
                }
            ],
            "dbEvents": [],
            "returnValue": null,
            "exception": null,
            "branchesTaken": ["if(condition)"],
            "loopsEntered": []
        },
        "startNanos": 0,
        "endNanos": 2000000
    });
    let req = test::TestRequest::post()
        .uri("/ingest")
        .set_json(&flow_tree)
        .to_request();
    let resp = test::call_service(&app, req).await;
    assert!(resp.status().is_success());
}

#[actix_web::test]
async fn test_ingest_with_db_events() {
    let app = test::init_service(App::new().service(ingest)).await;
    let flow_tree = json!({
        "traceId": "db123",
        "rootNode": {
            "methodDetails": {
                "className": "com.mycompany.Repository",
                "methodName": "findById",
                "methodSignature": "(Ljava/lang/Long;)Ljava/lang/Object;",
                "parameters": {"id": 123}
            },
            "executionDetails": {
                "startNanos": 0,
                "endNanos": 500000,
                "threadInfo": {
                    "threadId": 1,
                    "threadName": "main",
                    "isVirtual": false
                }
            },
            "codeAnalysis": {
                "instructionCount": 12,
                "maxLocalVariables": 4,
                "conditionalBranches": []
            },
            "children": [],
            "dbEvents": [
                {
                    "sql": "SELECT * FROM users WHERE id = ?",
                    "durationNanos": 300000,
                    "rowCount": 1
                }
            ],
            "returnValue": {"id": 123, "name": "John"},
            "exception": null,
            "branchesTaken": [],
            "loopsEntered": []
        },
        "startNanos": 0,
        "endNanos": 500000
    });
    let req = test::TestRequest::post()
        .uri("/ingest")
        .set_json(&flow_tree)
        .to_request();
    let resp = test::call_service(&app, req).await;
    assert!(resp.status().is_success());
}

#[actix_web::test]
async fn test_ingest_with_exception() {
    let app = test::init_service(App::new().service(ingest)).await;
    let flow_tree = json!({
        "traceId": "error123",
        "rootNode": {
            "methodDetails": {
                "className": "com.mycompany.Service",
                "methodName": "processData",
                "methodSignature": "(Ljava/lang/String;)V",
                "parameters": {"data": "invalid"}
            },
            "executionDetails": {
                "startNanos": 0,
                "endNanos": 100000,
                "threadInfo": {
                    "threadId": 1,
                    "threadName": "main",
                    "isVirtual": false
                }
            },
            "codeAnalysis": {
                "instructionCount": 5,
                "maxLocalVariables": 2,
                "conditionalBranches": []
            },
            "children": [],
            "dbEvents": [],
            "returnValue": null,
            "exception": {
                "type": "java.lang.IllegalArgumentException",
                "message": "Invalid data format"
            },
            "branchesTaken": [],
            "loopsEntered": []
        },
        "startNanos": 0,
        "endNanos": 100000
    });
    let req = test::TestRequest::post()
        .uri("/ingest")
        .set_json(&flow_tree)
        .to_request();
    let resp = test::call_service(&app, req).await;
    assert!(resp.status().is_success());
}

#[actix_web::test]
async fn test_ingest_with_filtering() {
    let app = test::init_service(App::new().service(ingest)).await;
    let flow_tree = json!({
        "traceId": "filter123",
        "rootNode": {
            "methodDetails": {
                "className": "com.mycompany.Controller",
                "methodName": "handle",
                "methodSignature": "()V",
                "parameters": {}
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
                "conditionalBranches": []
            },
            "children": [
                {
                    "methodDetails": {
                        "className": "org.springframework.web.servlet.DispatcherServlet",
                        "methodName": "doDispatch",
                        "methodSignature": "()V",
                        "parameters": {}
                    },
                    "executionDetails": {
                        "startNanos": 100000,
                        "endNanos": 900000,
                        "threadInfo": {
                            "threadId": 1,
                            "threadName": "main",
                            "isVirtual": false
                        }
                    },
                    "codeAnalysis": {
                        "instructionCount": 20,
                        "maxLocalVariables": 5,
                        "conditionalBranches": []
                    },
                    "children": [],
                    "dbEvents": [],
                    "returnValue": null,
                    "exception": null,
                    "branchesTaken": [],
                    "loopsEntered": []
                }
            ],
            "dbEvents": [],
            "returnValue": null,
            "exception": null,
            "branchesTaken": [],
            "loopsEntered": []
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