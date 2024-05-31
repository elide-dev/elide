use serde::{Deserialize, Serialize};

#[typeshare::typeshare]
#[derive(Clone, Hash, Eq, PartialEq, Debug, Serialize, Deserialize)]
pub struct DiagnosticNote {
  pub id: &'static str,
  pub message: &'static str
}
