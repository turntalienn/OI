use actix_web::{web, App, HttpServer, Responder, HttpResponse, post};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;

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

#[derive(Serialize)]
struct SpiralPoint {
    x: f64,
    y: f64,
    depth: usize,
    method: String,
}

fn filter_flow_tree(tree: &FlowTree, exclude_packages: &[String]) -> FlowTree {
    // Deep clone and filter the tree
    let mut filtered_tree = tree.clone();
    filtered_tree.root_node = filter_node(&tree.root_node, exclude_packages);
    filtered_tree
}

fn filter_node(node: &model::FlowCallNode, exclude_packages: &[String]) -> model::FlowCallNode {
    let mut filtered_children = Vec::new();
    
    for child in &node.children {
        // Check if this node should be excluded
        let should_exclude = exclude_packages.iter().any(|pkg| {
            child.method_details.class_name.starts_with(pkg)
        });
        
        if !should_exclude {
            let filtered_child = filter_node(child, exclude_packages);
            filtered_children.push(filtered_child);
        }
    }
    
    // Create a new node with filtered children
    model::FlowCallNode {
        method_details: node.method_details.clone(),
        execution_details: node.execution_details.clone(),
        code_analysis: node.code_analysis.clone(),
        children: filtered_children,
        db_events: node.db_events.clone(),
        return_value: node.return_value.clone(),
        exception: node.exception.clone(),
        branches_taken: node.branches_taken.clone(),
        loops_entered: node.loops_entered.clone(),
    }
}

fn quantum_collapse(tree: &FlowTree) -> Vec<BranchProbability> {
    let mut branch_counts: HashMap<String, usize> = HashMap::new();
    let mut total_branches = 0;
    
    // Count all branches in the tree
    count_branches_recursive(&tree.root_node, &mut branch_counts, &mut total_branches);
    
    // Convert to probabilities
    branch_counts
        .into_iter()
        .map(|(branch, count)| {
            let probability = if total_branches > 0 {
                count as f64 / total_branches as f64
            } else {
                0.0
            };
            BranchProbability { branch, p: probability }
        })
        .collect()
}

fn count_branches_recursive(node: &model::FlowCallNode, branch_counts: &mut HashMap<String, usize>, total: &mut usize) {
    // Count conditional branches from code analysis
    for branch in &node.code_analysis.conditional_branches {
        *branch_counts.entry(branch.clone()).or_insert(0) += 1;
        *total += 1;
    }
    
    // Count branches taken during execution
    for branch in &node.branches_taken {
        *branch_counts.entry(branch.clone()).or_insert(0) += 1;
        *total += 1;
    }
    
    // Recursively process children
    for child in &node.children {
        count_branches_recursive(child, branch_counts, total);
    }
}

fn generate_spiral_svg(tree: &FlowTree) -> String {
    let points = generate_spiral_points(&tree.root_node, 0, 0.0, 0.0);
    
    let mut svg = String::from("<svg width=\"800\" height=\"600\" xmlns=\"http://www.w3.org/2000/svg\">");
    svg.push_str("<defs><radialGradient id=\"spiralGradient\" cx=\"50%\" cy=\"50%\" r=\"50%\">");
    svg.push_str("<stop offset=\"0%\" style=\"stop-color:#4a90e2;stop-opacity:1\" />");
    svg.push_str("<stop offset=\"100%\" style=\"stop-color:#357abd;stop-opacity:0.3\" />");
    svg.push_str("</radialGradient></defs>");
    
    // Draw spiral path
    svg.push_str("<path d=\"M 400 300 ");
    for (i, point) in points.iter().enumerate() {
        let x = 400.0 + point.x * 50.0;
        let y = 300.0 + point.y * 50.0;
        if i == 0 {
            svg.push_str(&format!("L {} {}", x, y));
        } else {
            svg.push_str(&format!(" L {} {}", x, y));
        }
    }
    svg.push_str("\" stroke=\"url(#spiralGradient)\" stroke-width=\"3\" fill=\"none\" />");
    
    // Draw points
    for point in points {
        let x = 400.0 + point.x * 50.0;
        let y = 300.0 + point.y * 50.0;
        let radius = 5.0 + (point.depth as f64 * 2.0);
        svg.push_str(&format!(
            "<circle cx=\"{}\" cy=\"{}\" r=\"{}\" fill=\"#4a90e2\" opacity=\"0.8\" />",
            x, y, radius
        ));
        
        // Add method name as tooltip
        let method_name = point.method.split('.').last().unwrap_or(&point.method);
        svg.push_str(&format!(
            "<title>{}</title>",
            method_name
        ));
    }
    
    svg.push_str("</svg>");
    svg
}

fn generate_spiral_points(node: &model::FlowCallNode, depth: usize, angle: f64, radius: f64) -> Vec<SpiralPoint> {
    let mut points = Vec::new();
    
    // Calculate position for this node
    let x = radius * angle.cos();
    let y = radius * angle.sin();
    
    points.push(SpiralPoint {
        x,
        y,
        depth,
        method: node.method_details.method_name.clone(),
    });
    
    // Recursively process children with increasing angle and radius
    let child_angle_step = 0.5;
    let radius_step = 1.0;
    
    for (i, child) in node.children.iter().enumerate() {
        let child_angle = angle + (i as f64 * child_angle_step);
        let child_radius = radius + radius_step;
        let child_points = generate_spiral_points(child, depth + 1, child_angle, child_radius);
        points.extend(child_points);
    }
    
    points
}

#[post("/ingest")]
async fn ingest(tree: web::Json<FlowTree>) -> impl Responder {
    // In a real app, config would be loaded from a file.
    let exclude_packages = vec!["org.springframework".to_string()];

    // 1. Filter - Remove unwanted packages
    let filtered_tree = filter_flow_tree(&tree, &exclude_packages);

    // 2. Quantum Collapse - Calculate branch probabilities
    let branch_probabilities = quantum_collapse(&filtered_tree);

    // 3. Spiral Computation - Generate spiral visualization
    let spiral_svg = generate_spiral_svg(&filtered_tree);

    // 4. NL Narrative
    let summary = generate_summary(&filtered_tree.root_node);
    let steps = generate_steps(&filtered_tree.root_node, &mut 1);

    // 5. Response
    let response = ExplanationResponse {
        summary,
        steps,
        spiral_svg,
        branch_probabilities,
    };

    HttpResponse::Ok().json(response)
}

fn generate_summary(root: &model::FlowCallNode) -> String {
    let duration_ms = (root.execution_details.end_nanos - root.execution_details.start_nanos) / 1_000_000;
    let status = if root.exception.is_some() { "failed" } else { "completed successfully" };
    
    format!(
        "The request was handled by the entry point `{}` in class `{}`. It took {} ms and {}.",
        root.method_details.method_name,
        root.method_details.class_name,
        duration_ms,
        status
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