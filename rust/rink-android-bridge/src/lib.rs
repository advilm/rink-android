uniffi::setup_scaffolding!();

use std::sync::Mutex;

static RINK_CONTEXT: Mutex<Option<rink_core::Context>> = Mutex::new(None);

/// Initialize the Rink context once.
fn with_context<T>(
    f: impl FnOnce(&mut rink_core::Context) -> Result<T, String>,
) -> Result<T, String> {
    let mut guard = RINK_CONTEXT
        .lock()
        .map_err(|_| "Rink context lock was poisoned".to_string())?;

    if guard.is_none() {
        let mut ctx = rink_core::simple_context().map_err(|e| format!("{e}"))?;

        ctx.save_previous_result = true;

        *guard = Some(ctx);
    }

    f(guard.as_mut().unwrap())
}

/// Internal Rust API; also used by tests.
pub fn evaluate(query: &str) -> Result<String, String> {
    with_context(|ctx| rink_core::one_line(ctx, query).map_err(|e| format!("{e}")))
}

/// Evaluate a query for live preview without updating `ans`.
pub fn preview(query: &str) -> Result<String, String> {
    with_context(|ctx| {
        ctx.save_previous_result = false;

        let result = rink_core::one_line(ctx, query);

        ctx.save_previous_result = true;
        result
    })
}

/// API for live evaluation from Kotlin, committing `ans`.
#[uniffi::export]
pub fn evaluate_for_kotlin(query: String) -> String {
    match evaluate(&query) {
        Ok(result) => result,
        Err(error) => format!("Error: {error}"),
    }
}

/// API for live evaluation from Kotlin, without committing `ans`.
#[uniffi::export]
pub fn preview_for_kotlin(query: String) -> String {
    match preview(&query) {
        Ok(result) => result,
        Err(error) => format!("Error: {error}"),
    }
}

/// API for loading live currency data from Kotlin.
#[uniffi::export]
pub fn load_currency_data_for_kotlin(live_data: String) -> String {
    let result = with_context(|ctx| {
        let base_defs = rink_core::CURRENCY_FILE
            .ok_or_else(|| "Bundled currency data is unavailable".to_string())?;

        ctx.load_currency(Some(&live_data), base_defs)
            .map_err(|e| e.to_string())
    });

    match result {
        Ok(()) => "OK".to_string(),
        Err(error) => format!("Error: {error}"),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn evaluates_conversion() {
        let result = evaluate("1 + 1").unwrap();
        assert_eq!(result, "2 (dimensionless)");
    }

    #[test]
    fn evaluates_ans() {
        let _ = evaluate("1 + 1").unwrap();
        let result = evaluate("ans * 2").unwrap();
        assert_eq!(result, "4 (dimensionless)");
    }

    #[test]
    fn previews_without_updating_ans() {
        let _ = evaluate("1 + 1").unwrap();
        let _ = preview("ans * 2").unwrap();
        let result = evaluate("ans").unwrap();
        assert_eq!(result, "2 (dimensionless)");
    }
}
