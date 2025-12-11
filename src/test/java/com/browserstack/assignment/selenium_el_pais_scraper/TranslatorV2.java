package com.browserstack.assignment.selenium_el_pais_scraper;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple translator that uses the Google Translate v2 REST API.
 *
 * This class sends a single POST request containing a JSON body with multiple
 * "q" items (batch) to translate multiple Spanish texts to English in one call.
 *
 * Important: API key is hard-coded here as requested; in real projects prefer
 * env vars or secrets.
 */
public class TranslatorV2 {

	private static final Logger logger = LoggerFactory.getLogger(TranslatorV2.class);

	// <-- REPLACE this with your Google API key BEFORE running if you want
	// translations to work -->
	private static final String API_KEY = "AIzaSyDL0ki0HLkt-Qtuyuv9KfRv23q9unFmRBY";

	/** Base endpoint for Google Translate v2 (key appended at runtime). */
	private static final String ENDPOINT = "https://translation.googleapis.com/language/translate/v2?key=";

	private final Gson gson = new Gson();

	/**
	 * Translate a batch of Spanish texts to English.
	 *
	 * @param spanishTexts list of Spanish strings to translate. Null or empty list
	 *                     -> empty result.
	 * @return list of translated English strings (same size & order as input).
	 * @throws RuntimeException when the HTTP request fails or the response is
	 *                          malformed.
	 */
	public List<String> translateToEnglish(List<String> spanishTexts) {
		try {
			if (spanishTexts == null || spanishTexts.isEmpty()) {
				return new ArrayList<>();
			}

			// Build full URL (API key appended). Consider avoiding URL-encoding the key in
			// production.
			String urlStr = ENDPOINT + java.net.URLEncoder.encode(API_KEY, StandardCharsets.UTF_8);
			URL url = new URL(urlStr);

			// Build JSON body: { "q": [...], "source": "es", "target": "en", "format":
			// "text" }
			JsonObject body = new JsonObject();
			JsonArray q = new JsonArray();
			for (String t : spanishTexts)
				q.add(t == null ? "" : t);
			body.add("q", q);
			body.addProperty("source", "es");
			body.addProperty("target", "en");
			body.addProperty("format", "text");

			String jsonBody = gson.toJson(body);

			// Open connection and set request properties
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setConnectTimeout(15_000);
			conn.setReadTimeout(30_000);
			conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

			// Send JSON payload
			try (OutputStream os = conn.getOutputStream()) {
				os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
			}

			// Read response (success -> input stream; error -> error stream)
			int status = conn.getResponseCode();
			InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();

			StringBuilder sb = new StringBuilder();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
				String line;
				while ((line = br.readLine()) != null)
					sb.append(line).append('\n');
			}
			String response = sb.toString();

			// Non-2xx -> treat as error with full response for easier debugging
			if (status < 200 || status >= 300) {
				throw new RuntimeException("Translate API v2 HTTP " + status + " response: " + response);
			}

			// Parse JSON and extract translations array
			JsonObject json = gson.fromJson(response, JsonObject.class);
			JsonObject data = json.getAsJsonObject("data");
			var translations = data.getAsJsonArray("translations");

			List<String> out = new ArrayList<>();
			for (int i = 0; i < translations.size(); i++) {
				JsonObject item = translations.get(i).getAsJsonObject();
				String translated = item.has("translatedText") ? item.get("translatedText").getAsString() : "";
				out.add(translated);
			}

			// Sanity check: ensure the API returned the expected number of translations
			if (out.size() != spanishTexts.size()) {
				throw new RuntimeException("Translate API returned unexpected number of items. Resp: " + response);
			}

			conn.disconnect();
			return out;

		} catch (RuntimeException re) {
			// Re-throw runtime exceptions to preserve message and stack
			throw re;
		} catch (Exception e) {
			// Wrap checked exceptions in a RuntimeException for simplicity
			logger.error("Translation failed: {}", e.getMessage());
			throw new RuntimeException("Translation failed: " + e.getMessage(), e);
		}
	}
}
