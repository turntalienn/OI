use actix_web::{web, App, HttpServer, Responder, HttpResponse, post};
use serde::{Deserialize, Serialize};

mod model;
use model::FlowTree;

#[derive(Deserialize)]
struct Config {
    filter: FilterConfig,
}

#[derive(Deserialize)]
struct FilterConfig {
    exclude_packages: Vec<String>,
}

#[derive(Serialize)]
struct ExplanationResponse {
    summary: String,
    steps: Vec<Step>,
    spiral_svg: String,
    branch_probabilities: Vec<BranchProbability>,
}

#[derive(Serialize)]
struct Step {
    step: usize,
    description: String,
}

#[derive(Serialize)]
struct BranchProbability {
    branch: String,
    p: f64,
}


#[post("/ingest")]
async fn ingest(tree: web::Json<FlowTree>) -> impl Responder {
    // In a real app, config would be loaded from a file.
    let exclude_packages = vec!["org.springframework".to_string()];

    // 1. (Not implemented) Filter
    // 2. (Not implemented) Quantum Collapse
    // 3. (Not implemented) Spiral Computation

    // 4. NL Narrative
    let summary = generate_summary(&tree.root_node);
    let steps = generate_steps(&tree.root_node, &mut 1);

    // 5. Response
    let response = ExplanationResponse {
        summary,
        steps,
        spiral_svg: "<svg>...</svg>".to_string(), // Placeholder
        branch_probabilities: vec![],           // Placeholder
    };

    HttpResponse::Ok().json(response)
}

fn generate_summary(root: &model::FlowCallNode) -> String {
    format!(
        "The request was handled by the entry point `{}` in class `{}`. It took {} ms and completed successfully.",
        root.method_details.method_name,
        root.method_details.class_name,
        (root.execution_details.end_nanos - root.execution_details.start_nanos) / 1_000_000
    )
}

fn generate_steps(node: &model::FlowCallNode, step_counter: &mut usize) -> Vec<Step> {
    let mut steps = Vec::new();
    let duration = (node.execution_details.end_nanos - node.execution_details.start_nanos) / 1_000_000;
    
    let description = format!(
        "Call to `{}` on thread `{}` took {} ms.",
        node.method_details.method_name,
        node.execution_details.thread_info.thread_name,
        duration
    );

    steps.push(Step { step: *step_counter, description });
    *step_counter += 1;

    // Display DB events that occurred within this method call
    for db_event in &node.db_events {
        let db_duration = db_event.duration_nanos / 1_000_000;
        let sql_snippet: String = db_event.sql.chars().take(80).collect();
        let db_description = format!(
            "  └─ DB query took {} ms. SQL: `{}...`",
            db_duration,
            sql_snippet
        );
        steps.push(Step { step: *step_counter, description: db_description });
        *step_counter += 1;
    }

    for child in &node.children {
        steps.extend(generate_steps(child, step_counter));
    }

    steps
}


#[actix_web::main]
async fn main() -> std::io::Result<()> {
    println!("Starting oi-explainer server at http://127.0.0.1:8081");
    HttpServer::new(|| {
        App::new().service(ingest)
    })
    .bind(("127.0.0.1", 8081))?
    .run()
    .await
} 