package com.browserstack.assignment.selenium_el_pais_scraper;

import java.util.*;

/**
 * Provides utility methods for analyzing text extracted from scraped articles.
 *
 * Currently, this class focuses on counting repeated words across translated
 * English titles. Titles are normalized to lowercase, cleaned of punctuation,
 * split into words, and counted. The caller may specify a threshold to filter
 * out infrequent words.
 */
public class TextAnalyzer {

	/**
	 * Counts repeated words across a list of English titles and returns those whose
	 * frequency exceeds the given threshold.
	 *
	 * Processing steps for each title: - Convert to lowercase - Remove punctuation
	 * (non-letter characters), keeping spaces - Split into words - Increment word
	 * counts
	 *
	 * @param titlesEn  List of English-translated titles to analyze.
	 * @param threshold Only return words that appear more than this number.
	 * @return A map of word → count for all words exceeding the threshold. Returns
	 *         an empty map if the input list is null or empty.
	 */
	public Map<String, Integer> repeatedWords(List<String> titlesEn, int threshold) {
		if (titlesEn == null || titlesEn.isEmpty())
			return Collections.emptyMap();

		// Preserve insertion order
		Map<String, Integer> counts = new LinkedHashMap<>();

		// Count words across all titles
		for (String title : titlesEn) {
			if (title == null)
				continue;

			String lower = title.toLowerCase();
			String cleaned = lower.replaceAll("[^\\p{L}\\s]", " "); // keep letters, replace punctuation
			String[] words = cleaned.split("\\s+");

			for (String w : words) {
				if (w == null)
					continue;
				w = w.trim();
				if (w.isEmpty())
					continue; // avoid counting empty strings

				counts.put(w, counts.getOrDefault(w, 0) + 1);
			}
		}

		// Filter words exceeding the threshold
		Map<String, Integer> result = new LinkedHashMap<>();
		for (Map.Entry<String, Integer> e : counts.entrySet()) {
			if (e.getValue() > threshold) {
				result.put(e.getKey(), e.getValue());
			}
		}
		return result;
	}
}
