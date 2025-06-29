use serde::Deserialize;
use std::collections::HashMap;

#[derive(Debug, Deserialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct FlowTree {
    pub trace_id: String,
    pub root_node: FlowCallNode,
    pub start_nanos: u64,
    pub end_nanos: u64,
}

#[derive(Debug, Deserialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct FlowCallNode {
    pub method_details: MethodDetails,
    pub execution_details: ExecutionDetails,
    #[serde(default)]
    pub code_analysis: CodeAnalysis,
    #[serde(default)]
    pub children: Vec<FlowCallNode>,
    #[serde(default)]
    pub db_events: Vec<DbQueryEvent>,
    // returnValue and exception are tricky to type; we'll use serde_json::Value
    pub return_value: Option<serde_json::Value>,
    pub exception: Option<serde_json::Value>,
    #[serde(default)]
    pub branches_taken: Vec<String>,
    #[serde(default)]
    pub loops_entered: Vec<String>,
}

#[derive(Debug, Deserialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct MethodDetails {
    pub class_name: String,
    pub method_name: String,
    pub method_signature: String,
    #[serde(default)]
    pub parameters: HashMap<String, serde_json::Value>,
}

#[derive(Debug, Deserialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct ExecutionDetails {
    pub start_nanos: u64,
    pub end_nanos: u64,
    pub thread_info: ThreadInfo,
}

#[derive(Debug, Deserialize, Default, Clone)]
#[serde(rename_all = "camelCase")]
pub struct CodeAnalysis {
    pub instruction_count: i32,
    pub max_local_variables: i32,
    #[serde(default)]
    pub conditional_branches: Vec<String>,
}

#[derive(Debug, Deserialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct ThreadInfo {
    pub thread_id: u64,
    pub thread_name: String,
    pub is_virtual: bool,
}

#[derive(Debug, Deserialize, Clone)]
#[serde(rename_all = "camelCase")]
pub struct DbQueryEvent {
    pub sql: String,
    pub duration_nanos: u64,
    pub row_count: i32,
} 