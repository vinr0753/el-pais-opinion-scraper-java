package com.browserstack.assignment.selenium_el_pais_scraper;

import org.openqa.selenium.By;

/**
 * Centralized repository of XPath/CSS selectors used across the scraper.
 * Keeping them here improves maintainability and readability of the scraping
 * logic.
 */
public class ElPaisElements {

	/**
	 * Selector for verifying that the UI language is set to Spanish. Looks for:
	 * "Seleccione:" → following sibling div → span with text "España".
	 */
	public static final By LANGUAGE_SPAN = By
			.xpath("//*[text()=\"Seleccione:\"]/following-sibling::div//span[text()=\"España\"]");

	/** Selector for the "Opinión" navigation link in the top nav bar. */
	public static final By OPINION_NAV = By.xpath("//nav[@class=\"cs_m\"]//a[text()=\"Opinión\"]");

	/** Selector validating that the Opinion page loaded correctly (header text). */
	public static final By OPINION_HEADER = By.xpath("//h1/a[text()=\"Opinión\"]");

	/** Selector for all article elements in the listing page. */
	public static final By ALL_ARTICLES = By.xpath("//article");

	/** Selector for article links inside the article listing. */
	public static final By ARTICLE_LINKS = By.xpath("//article//h2/a");

	/** Selector for the article title displayed on the article page. */
	public static final By ARTICLE_TITLE = By.xpath("//h1");

	/** Selector for the header image inside an article, if present. */
	public static final By ARTICLE_IMAGE = By.xpath("//article/header//img");

	/**
	 * Selector for the first paragraph of the article body. Used for translation or
	 * summarization.
	 */
	public static final By FIRST_PARAGRAPH = By.xpath("(//header/following-sibling::div/p)[1]");

	/**
	 * Generic cookie-accept button selector. Matches Didomi popup or other "Accept"
	 * / "Aceptar" variations.
	 */
	public static final By COOKIE_POPUP_BTN = By
			.xpath("//button[@id='didomi-notice-agree-button' or contains(., 'Aceptar') or contains(., 'Accept')]");
}
