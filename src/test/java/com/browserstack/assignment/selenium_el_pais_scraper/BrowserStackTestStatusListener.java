package com.browserstack.assignment.selenium_el_pais_scraper;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TestNG listener that marks BrowserStack session status after each test.
 *
 * Behavior: - onTestSuccess -> set status 'passed' - onTestFailure -> set
 * status 'failed' and include failure message - onTestSkipped -> set status
 * 'skipped'
 *
 * It uses DriverFactory.getDriver() to obtain the ThreadLocal WebDriver and
 * then executes the special BrowserStack JS command: browserstack_executor:
 * {"action":"setSessionStatus","arguments":{"status":"passed","reason":"..."}}
 *
 * After successfully setting the status the listener calls
 * DriverFactory.markStatusSet() so the DriverFactory teardown doesn't need to
 * do a fallback set.
 */
public class BrowserStackTestStatusListener implements ITestListener {

	private static final Logger logger = LoggerFactory.getLogger(BrowserStackTestStatusListener.class);

	/**
	 * Helper to set status through the BrowserStack executor. Marks the
	 * DriverFactory flag on success.
	 */
	private void setStatus(String status, String reason) {
		WebDriver driver = DriverFactory.getDriver();
		if (driver == null) {
			logger.warn("No WebDriver available to set BrowserStack status: {} - {}", status, reason);
			return;
		}
		if (!(driver instanceof JavascriptExecutor)) {
			logger.warn("Driver is not JavascriptExecutor; cannot set BrowserStack status.");
			return;
		}

		try {
			// Escape reason for safe JSON inside the JS string
			String script = "browserstack_executor: {\"action\": \"setSessionStatus\", \"arguments\": {\"status\":\""
					+ status + "\", \"reason\": \"" + escapeForJson(reason) + "\"}}";
			((JavascriptExecutor) driver).executeScript(script);
			logger.info("Set BrowserStack session status: {} ({})", status, reason);
			// mark that status was set by listener so teardown does not override it
			DriverFactory.markStatusSet();
		} catch (Exception e) {
			logger.error("Failed to set BrowserStack session status: {}", e.getMessage());
		}
	}

	private String escapeForJson(String s) {
		if (s == null)
			return "";
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
	}

	@Override
	public void onTestSuccess(ITestResult result) {
		setStatus("passed", "Test passed");
	}

	@Override
	public void onTestFailure(ITestResult result) {
		String reason = result.getThrowable() != null ? result.getThrowable().toString() : "Test failed";
		setStatus("failed", reason);
	}

	@Override
	public void onTestSkipped(ITestResult result) {
		String reason = result.getThrowable() != null ? result.getThrowable().toString() : "Test skipped";
		setStatus("skipped", reason);
	}

	// Other ITestListener methods left as no-ops; implement if needed.
	@Override
	public void onStart(ITestContext context) {
	}

	@Override
	public void onFinish(ITestContext context) {
	}

	@Override
	public void onTestStart(ITestResult result) {
	}

	@Override
	public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
	}
}
