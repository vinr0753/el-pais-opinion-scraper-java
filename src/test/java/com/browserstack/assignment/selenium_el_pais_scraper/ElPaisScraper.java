package com.browserstack.assignment.selenium_el_pais_scraper;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ElPaisScraper - main scraping logic.
 *
 * Responsibilities: - Use DriverFactory.getDriver() to obtain a WebDriver for
 * the current thread. - Navigate to elpais.com, accept cookie banner if
 * present. - Navigate to "Opinión" section (click nav or fallback to direct
 * URL). - Collect the first N opinion article links and visit them, saving
 * title, first paragraph, image. - Bulk-translate titles using TranslatorV2
 * (minimize API calls). - Analyze English titles for repeated words using
 * TextAnalyzer.
 *
 * The class extends DriverFactory so that DriverFactory's @BeforeMethod
 * and @AfterMethod lifecycle methods are executed for this test class, ensuring
 * drivers are created.
 */
@Test
public class ElPaisScraper extends DriverFactory {

	private static final Logger logger = LoggerFactory.getLogger(ElPaisScraper.class);

	/** How many articles to fetch (first N opinion articles). */
	private static final int MAX_ARTICLES = 5;

	/** Fallback URL to the Opinión section when nav link is not found. */
	private static final String OPINION_URL = "https://elpais.com/opinion/";

	/**
	 * Main test method executed by TestNG. It performs the entire scraping workflow
	 * for the single session provided by DriverFactory.
	 */
	public void startScraper() throws Exception {

		WebDriver driver = DriverFactory.getDriver();
		if (driver == null) {
			logger.error("No WebDriver available for this test thread. Aborting scraper.");
			return;
		}

		// Set a short implicit wait for convenience; explicit waits are used for
		// specific elements.
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));

		// 2) Navigate to homepage and wait for full load
		driver.get("https://elpais.com/");
		waitForPageLoad(driver);

		// Accept cookie popup if present (robust but non-fatal)
		acceptCookies(driver);

		// 3) Check language is Spanish (optional info)
		WebElement spainEl = waitForElement(driver, ElPaisElements.LANGUAGE_SPAN, 5);
		if (spainEl != null) {
			logger.info("[Language] 'España' found — page likely Spanish.");
		} else {
			logger.info("[Language] 'España' not found via selector (page may already be Spanish).");
		}

		// 4) Navigate to Opinión (click link if present; otherwise navigate directly)
		WebElement opinionNav = waitForElement(driver, ElPaisElements.OPINION_NAV, 6);
		if (opinionNav != null) {
			logger.info("[Nav] Clicking 'Opinión' link...");
			try {
				opinionNav.click();
				waitForPageLoad(driver);
			} catch (Exception e) {
				// If click fails for any reason, fallback to direct URL to continue scraping
				logger.warn("[Nav] Failed to click opinion link: {}. Falling back to direct URL.", e.getMessage());
				driver.get(OPINION_URL);
				waitForPageLoad(driver);
			}
		} else {
			logger.info("[Nav] 'Opinión' link not found — navigating directly to {}", OPINION_URL);
			driver.get(OPINION_URL);
			waitForPageLoad(driver);
		}

		// Determine we are on the opinion page (either header found or URL path check)
		boolean onOpinion = false;
		WebElement opinionHeader = waitForElement(driver, ElPaisElements.OPINION_HEADER, 5);
		if (opinionHeader != null)
			onOpinion = true;
		else if (driver.getCurrentUrl() != null && driver.getCurrentUrl().startsWith(OPINION_URL))
			onOpinion = true;

		logger.info("[Page] On Opinión page? {} (URL: {})", onOpinion, driver.getCurrentUrl());

		// 5) Count <article> elements on listing page (informational)
		List<WebElement> articleEls = driver.findElements(ElPaisElements.ALL_ARTICLES);
		logger.info("[Articles] <article> elements found on page: {}", articleEls.size());

		// 6) Collect first MAX_ARTICLES article links (//article//h2/a)
		List<WebElement> linkEls = driver.findElements(ElPaisElements.ARTICLE_LINKS);
		List<String> articleLinks = new ArrayList<>();
		for (WebElement l : linkEls) {
			try {
				String href = l.getAttribute("href");
				// Keep only opinion articles and avoid duplicates
				if (href != null && href.contains("/opinion/") && !articleLinks.contains(href)) {
					articleLinks.add(href);
					if (articleLinks.size() >= MAX_ARTICLES)
						break;
				}
			} catch (Exception ignored) {
			}
		}

		int storedCount = Math.min(articleLinks.size(), MAX_ARTICLES);
		logger.info("[Links] Storing first {} article URLs:", storedCount);
		for (int i = 0; i < storedCount; i++) {
			logger.info("  {}) {}", i + 1, articleLinks.get(i));
		}

		// Prepare images download folder: ~/elpaisscraper/<timestamp>/images
		String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		Path imagesFolder = Path.of(System.getProperty("user.home"), "elpaisscraper", ts, "images");
		try {
			Files.createDirectories(imagesFolder);
		} catch (Exception e) {
			logger.warn("Failed to create images folder: {}", e.getMessage());
		}

		// 7) Visit each stored link and collect data into Article objects
		List<Article> articles = new ArrayList<>();
		for (int idx = 0; idx < storedCount; idx++) {
			String url = articleLinks.get(idx);
			logger.info("\n=== Article {}/{} ===", idx + 1, storedCount);
			logger.info("URL: {}", url);

			try {
				driver.get(url);
				waitForPageLoad(driver);

				Article a = new Article(url);

				// 7.1 Title (//h1)
				WebElement titleEl = waitForElement(driver, ElPaisElements.ARTICLE_TITLE, 6);
				a.titleEs = (titleEl != null) ? titleEl.getText() : "";
				logger.info("Title (ES): {}", a.titleEs.isBlank() ? "(not found)" : a.titleEs);

				// 7.2 First paragraph for translation (print only)
				WebElement p1 = waitForElement(driver, ElPaisElements.FIRST_PARAGRAPH, 5);
				String firstPara = (p1 != null) ? p1.getText() : "";
				logger.info("First paragraph (ES): {}", firstPara.isBlank() ? "(not found)" : firstPara);

				// 7.3 Image url and download (//article/header//img)
				String imgUrl = null;
				String savedPath = null;
				try {
					WebElement img = waitForElement(driver, ElPaisElements.ARTICLE_IMAGE, 4);
					if (img != null) {
						// Robust extraction: try src, then data-src, data-lazy-src, then srcset
						imgUrl = firstNonBlank(img.getAttribute("src"), img.getAttribute("data-src"),
								img.getAttribute("data-lazy-src"));

						if ((imgUrl == null || imgUrl.isBlank()) && img.getAttribute("srcset") != null) {
							String srcset = img.getAttribute("srcset");
							String[] parts = srcset.split(",");
							String last = parts[parts.length - 1].trim();
							imgUrl = last.split("\\s+")[0];
						}

						logger.info("Image URL: {}", imgUrl == null ? "(none)" : imgUrl);

						// Download the image (DownloadUtil handles folder creation)
						if (imgUrl != null && !imgUrl.isBlank()) {
							savedPath = DownloadUtil.downloadImage(imgUrl, imagesFolder.toString());
							if (savedPath != null) {
								logger.info("Saved image to: {}", savedPath);
							} else {
								logger.warn("Saved image to: (download failed)");
							}
						}
					} else {
						logger.info("Image URL: (none)");
					}
				} catch (Exception ex) {
					logger.warn("Image: error while retrieving/downloading: {}", ex.getMessage());
				}

				a.imageUrl = imgUrl;
				// store article
				articles.add(a);

			} catch (Exception e) {
				logger.error("Error processing article {}: {}", url, e.getMessage(), e);
				logger.info("(Error processing this article; continuing to next.)");
			}

			// separation line for console readability
			logger.info("=========================================");
		} // end for each article

		// 8) Bulk translate all titles (minimize API calls)
		List<String> titlesEs = new ArrayList<>();
		for (Article a : articles)
			titlesEs.add(a.titleEs == null ? "" : a.titleEs);

		if (!titlesEs.isEmpty()) {
			try {
				// TranslatorV2 requires API key; ensure it's set
				TranslatorV2 translator = new TranslatorV2(); // ensure API_KEY set inside class
				List<String> titlesEn = translator.translateToEnglish(titlesEs);

				// Print numbered Original -> Translated (neat)
				logger.info("\n=== Translations (Titles) ===");
				for (int i = 0; i < articles.size(); i++) {
					String orig = articles.get(i).titleEs;
					String trans = (i < titlesEn.size()) ? titlesEn.get(i) : "";
					articles.get(i).titleEn = trans;
					logger.info("{}. Original:   {}", (i + 1), orig);
					logger.info("   Translated: {}", trans);
					logger.info("---------------------------------------");
				}

				// Analyze combined English titles for repeated words > 2 occurrences
				List<String> englishTitles = new ArrayList<>();
				for (Article a : articles)
					englishTitles.add(a.titleEn == null ? "" : a.titleEn);

				TextAnalyzer analyzer = new TextAnalyzer();
				Map<String, Integer> repeated = analyzer.repeatedWords(englishTitles, 2);

				logger.info("\n=== Words repeated more than twice across all English titles ===");
				if (repeated.isEmpty()) {
					logger.info("None found.");
				} else {
					repeated.forEach((w, c) -> logger.info("  {} -> {}", w, c));
				}

			} catch (Exception e) {
				logger.error("Translation step failed: {}", e.getMessage(), e);
				throw e; // rethrow so the failure is visible to TestNG and listener
			}
		} else {
			logger.info("[Translations] No titles available to translate.");
		}

	} // end startScraper

	// ------------------------------------------------------------------
	// Accept cookie popup if present. Uses centralized selector.
	// ------------------------------------------------------------------
	private void acceptCookies(WebDriver driver) {
		try {
			// Wait for the cookie accept button and click it
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
			WebElement cookieBtn = wait.until(ExpectedConditions.elementToBeClickable(ElPaisElements.COOKIE_POPUP_BTN));
			cookieBtn.click();
			wait.until(ExpectedConditions.invisibilityOf(cookieBtn));
			logger.info("[Cookie] Clicked cookie accept button.");
		} catch (Exception e) {
			// not fatal: continue without cookies accepted
			logger.debug("[Cookie] Cookie popup not found or could not click it. {}", e.getMessage());
		}
	}

	// ------------------------------------------------------------------
	// Wait for an element to become visible; returns null on timeout.
	// Overloaded to accept WebDriver (works for local RemoteWebDriver).
	// ------------------------------------------------------------------
	private WebElement waitForElement(WebDriver driver, By selector, int seconds) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(seconds));
			return wait.until(ExpectedConditions.visibilityOfElementLocated(selector));
		} catch (Exception e) {
			return null;
		}
	}

	// ------------------------------------------------------------------
	// Wait until document.readyState == "complete"; gentle and reusable.
	// ------------------------------------------------------------------
	private void waitForPageLoad(WebDriver driver) {
		try {
			new WebDriverWait(driver, Duration.ofSeconds(10)).until(
					wd -> ((JavascriptExecutor) wd).executeScript("return document.readyState").equals("complete"));
		} catch (Exception ignored) {
			// continue even if timed out
		}
	}

	// Helper: return the first non-blank string among args
	private static String firstNonBlank(String... candidates) {
		if (candidates == null)
			return null;
		for (String s : candidates) {
			if (s != null && !s.isBlank())
				return s;
		}
		return null;
	}
}
