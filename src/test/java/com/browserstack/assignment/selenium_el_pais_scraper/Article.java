package com.browserstack.assignment.selenium_el_pais_scraper;

/**
 * Simple POJO to store scraped article metadata. Fields are kept public for
 * simplicity (can be converted to private + getters/setters).
 */
public class Article {

	/** The URL of the original article. */
	public String url;

	/** The article title in Spanish. */
	public String titleEs;

	/** The article title translated into English. */
	public String titleEn;

	/** The main textual content of the article (not currently scraped). */
	public String content;

	/** The URL of the article's main image, if any. */
	public String imageUrl;

	/**
	 * Constructor to create an Article instance with the specified URL.
	 *
	 * @param url The URL from which the article was scraped.
	 */
	public Article(String url) {
		this.url = url;
	}
}
