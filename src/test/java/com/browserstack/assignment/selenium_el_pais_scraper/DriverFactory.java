package com.browserstack.assignment.selenium_el_pais_scraper;

import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.WebDriverException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * DriverFactory - provides WebDriver instances for tests.
 *
 * Key responsibilities: - Decide whether to run locally or on BrowserStack
 * (executionEnv param / system property). - For local runs: create a single
 * local browser (first test thread); skip other parallel threads. - For
 * BrowserStack: create RemoteWebDriver with appropriate bstack:options. - Keep
 * WebDriver instance per-thread in ThreadLocal. - Expose getDriver() so
 * tests/listeners can access the driver. - Track whether BrowserStack session
 * status was set (ThreadLocal flag) so teardown can fallback.
 *
 * NOTE: Credentials are hard-coded here per your request; for production use
 * env vars or secrets manager.
 */
public class DriverFactory {

	private static final Logger logger = LoggerFactory.getLogger(DriverFactory.class);

	// ThreadLocal to store driver per test thread
	private static final ThreadLocal<WebDriver> TL_DRIVER = new ThreadLocal<>();

	// ThreadLocal flag indicating whether status was already set for this session
	private static final ThreadLocal<Boolean> TL_STATUS_SET = ThreadLocal.withInitial(() -> Boolean.FALSE);

	// When running locally, only allow one thread to start the browser (prevent
	// redundant local browsers)
	private static final AtomicBoolean LOCAL_RUN_STARTED = new AtomicBoolean(false);

	// ---------- HARD-CODED BrowserStack credentials (replace outside source
	// control in real projects) ----------
	private static final String BS_USER = "vinodraj_3rbDXF";
	private static final String BS_KEY = "x2rC4uzRzf125jNMc9zp";
	private static final String BS_HUB = "https://hub.browserstack.com/wd/hub";
	// ---------------------------------------------------------------------------------------------------------

	// Browser option templates (can be tuned as required)
	private final FirefoxOptions firefoxOptions = new FirefoxOptions();
	private final ChromeOptions chromeOptions = new ChromeOptions();
	private final EdgeOptions edgeOptions = new EdgeOptions();

	// ----------------- Accessors for listener/other classes -----------------
	public static WebDriver getDriver() {
		return TL_DRIVER.get();
	}

	private static void setDriverInstance(WebDriver driver) {
		TL_DRIVER.set(driver);
	}

	private static void removeDriver() {
		TL_DRIVER.remove();
	}

	/** Mark that the listener set the BrowserStack session status. */
	public static void markStatusSet() {
		TL_STATUS_SET.set(Boolean.TRUE);
	}

	/** Check whether session status has been set already. */
	public static boolean isStatusSet() {
		return TL_STATUS_SET.get() != null && TL_STATUS_SET.get();
	}

	private static void clearStatusFlag() {
		TL_STATUS_SET.remove();
	}
	// -----------------------------------------------------------------------

	/**
	 * Initialize a WebDriver before each test method.
	 *
	 * TestNG parameters: - executionEnv: "browserstack" (default) or "local" -
	 * browserName/browserVersion/os/osVersion/deviceName etc for BrowserStack -
	 * browser for local browser choice
	 *
	 * The long @Parameters list allows passing capabilities per <test> from
	 * testng.xml.
	 */
	@BeforeMethod(alwaysRun = true)
	@Parameters({ "executionEnv", "browser", "browserName", "browserVersion", "os", "osVersion", "deviceName",
			"projectName", "buildName", "sessionName", "debug", "networkLogs", "consoleLogs" })
	public void initialize(@Optional("") String executionEnvParam, @Optional("chrome") String localBrowserParam,
			@Optional("") String browserNameParam, @Optional("") String browserVersionParam,
			@Optional("") String osParam, @Optional("") String osVersionParam, @Optional("") String deviceNameParam,
			@Optional("") String projectNameParam, @Optional("") String buildNameParam,
			@Optional("") String sessionNameParam, @Optional("false") String debugParam,
			@Optional("false") String networkLogsParam, @Optional("info") String consoleLogsParam) throws Exception {

		/*
		 * Resolve execution environment in this order: 1) TestNG suite parameter
		 * executionEnv (preferred) 2) System property executionEnv 3) System property
		 * remote (legacy) 4) default "browserstack"
		 */
		String exec = (executionEnvParam != null && !executionEnvParam.isBlank())
				? executionEnvParam.trim().toLowerCase()
				: System.getProperty("executionEnv", System.getProperty("remote", "browserstack")).trim().toLowerCase();

		if (exec == null || exec.isBlank())
			exec = "browserstack";
		exec = exec.toLowerCase();

		logger.info("Effective executionEnv = {}", exec);

		// ---------- LOCAL behavior: only allow a single thread to create a local
		// browser ----------
		if (!"browserstack".equals(exec)) {
			// Use atomic flag so only the first thread creates a local browser; others are
			// skipped.
			boolean isFirstLocal = LOCAL_RUN_STARTED.compareAndSet(false, true);
			if (!isFirstLocal) {
				// Skip this test/thread to avoid redundant local browsers.
				logger.info(
						"Local execution detected and another local session already started. Skipping this test thread to avoid redundant local sessions.");
				throw new SkipException(
						"Local run: only one session allowed. This test was skipped to avoid redundant local sessions.");
			}

			// The first local thread will create a local browser instance.
			String browser = (localBrowserParam == null || localBrowserParam.isBlank())
					? System.getProperty("browser", "chrome")
					: localBrowserParam;
			WebDriver localDriver;
			switch (browser.toLowerCase()) {
			case "firefox":
				WebDriverManager.firefoxdriver().setup();
				localDriver = new FirefoxDriver(firefoxOptions);
				break;
			case "edge":
				WebDriverManager.edgedriver().setup();
				localDriver = new EdgeDriver(edgeOptions);
				break;
			case "chrome":
			default:
				WebDriverManager.chromedriver().setup();
				localDriver = new ChromeDriver(chromeOptions);
				break;
			}

			// Configure sensible timeouts for local runs
			localDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
			localDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
			setDriverInstance(localDriver);
			logger.info("Launched SINGLE LOCAL browser: {}", browser);
			return;
		}

		// ---------- BrowserStack branch ----------
		// Assemble capabilities and bstack:options from TestNG params / system
		// properties
		String browserName = (browserNameParam == null || browserNameParam.isBlank())
				? System.getProperty("browserName", "Chrome")
				: browserNameParam;
		String browserVersion = (browserVersionParam == null || browserVersionParam.isBlank())
				? System.getProperty("browserVersion", "latest")
				: browserVersionParam;
		String os = (osParam == null) ? System.getProperty("os", "") : osParam;
		String osVersion = (osVersionParam == null) ? System.getProperty("osVersion", "") : osVersionParam;
		String deviceName = (deviceNameParam == null) ? System.getProperty("deviceName", "") : deviceNameParam;

		// Base capabilities (standard W3C)
		MutableCapabilities caps = new MutableCapabilities();
		caps.setCapability("browserName", browserName);
		caps.setCapability("browserVersion", browserVersion);

		// bstack:options (BrowserStack-specific metadata)
		Map<String, Object> bstackOptions = new HashMap<>();
		bstackOptions.put("userName", BS_USER);
		bstackOptions.put("accessKey", BS_KEY);

		String projectName = (projectNameParam == null || projectNameParam.isBlank()) ? "ElPaisScraper"
				: projectNameParam;
		String buildName = (buildNameParam == null || buildNameParam.isBlank())
				? ("ElPaisScraper - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")))
				: buildNameParam;

		bstackOptions.put("projectName", projectName);
		bstackOptions.put("buildName", buildName);

		String sessionName = (sessionNameParam == null || sessionNameParam.isBlank()) ? ("ElPaisScraper - "
				+ browserName + (deviceName != null && !deviceName.isBlank() ? " - " + deviceName : ""))
				: sessionNameParam;
		bstackOptions.put("sessionName", sessionName);

		// Optional booleans / logs
		if ("true".equalsIgnoreCase(debugParam) || "true".equalsIgnoreCase(System.getProperty("debug", "false"))) {
			bstackOptions.put("debug", true);
		}
		if ("true".equalsIgnoreCase(networkLogsParam)
				|| "true".equalsIgnoreCase(System.getProperty("networkLogs", "false"))) {
			bstackOptions.put("networkLogs", true);
		}
		if (consoleLogsParam != null && !consoleLogsParam.isBlank()) {
			bstackOptions.put("consoleLogs", consoleLogsParam);
		}

		// If mobile device specified use deviceName / osVersion; otherwise set
		// os/osVersion for desktop
		if (deviceName != null && !deviceName.isBlank()) {
			bstackOptions.put("deviceName", deviceName);
			if (osVersion != null && !osVersion.isBlank())
				bstackOptions.put("osVersion", osVersion);
		} else {
			if (os != null && !os.isBlank())
				bstackOptions.put("os", os);
			if (osVersion != null && !osVersion.isBlank())
				bstackOptions.put("osVersion", osVersion);
		}

		// Attach bstack:options to capabilities
		caps.setCapability("bstack:options", bstackOptions);

		logger.info("Creating BrowserStack session with capabilities: browserName={}, browserVersion={}", browserName,
				browserVersion);
		logger.debug("bstack:options = {}", bstackOptions);

		try {
			// Create RemoteWebDriver; BrowserStack will allocate the required environment
			WebDriver remoteDriver = new RemoteWebDriver(new URL(BS_HUB), caps);
			remoteDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
			remoteDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(120));
			setDriverInstance(remoteDriver);

			// Log the session id so you can quickly open Automate dashboard entry
			try {
				SessionId sid = ((RemoteWebDriver) remoteDriver).getSessionId();
				if (sid != null) {
					logger.info("BrowserStack sessionId = {}", sid.toString());
					logger.info("Automate session URL: https://automate.browserstack.com/sessions/{}", sid.toString());
					logger.info("Build grouping (project/build): {} / {}", projectName, buildName);
				} else {
					logger.warn("Session id is null after RemoteWebDriver creation.");
				}
			} catch (Exception ex) {
				logger.warn("Could not obtain session id: {}", ex.getMessage());
			}

			logger.info("BrowserStack session started.");
		} catch (WebDriverException wde) {
			// Surface useful debug information if session creation fails
			logger.error("Failed to create RemoteWebDriver session. bstack:options={}, hub={}, message={}",
					bstackOptions, BS_HUB, wde.getMessage());
			throw wde;
		}
	}

	/**
	 * Teardown called after each test method. - If the listener didn't set
	 * BrowserStack status, attempt a fallback setSessionStatus call. - Quit and
	 * cleanup the WebDriver and ThreadLocal flags.
	 */
	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		WebDriver d = getDriver();
		if (d != null) {
			try {
				// If listener did not set status, attempt fallback (set skipped by default)
				if (!isStatusSet()) {
					try {
						String reason = "Auto-mark by teardown (listener did not set status)";
						String script = "browserstack_executor: {\"action\": \"setSessionStatus\", \"arguments\": {\"status\":\"skipped\", \"reason\": \""
								+ reason.replace("\"", "\\\"") + "\"}}";
						if (d instanceof JavascriptExecutor) {
							((JavascriptExecutor) d).executeScript(script);
							logger.info("Teardown auto-set BrowserStack status: skipped");
						} else {
							logger.warn("Teardown: driver is not JavascriptExecutor, cannot set BrowserStack status.");
						}
					} catch (Exception ex) {
						logger.error("Teardown failed to set BrowserStack status: {}", ex.getMessage());
					}
				} else {
					logger.debug("Teardown: BrowserStack status already set by listener.");
				}
			} catch (Exception ex) {
				logger.warn("Unexpected error during teardown status check: {}", ex.getMessage());
			} finally {
				try {
					d.quit();
					logger.info("Driver quit successfully.");
				} catch (Exception ignored) {
					logger.warn("Exception while quitting driver: {}", ignored.getMessage());
				}
				removeDriver();
				clearStatusFlag();
			}
		} else {
			logger.debug("Teardown: no WebDriver found for this thread.");
		}
	}
}
