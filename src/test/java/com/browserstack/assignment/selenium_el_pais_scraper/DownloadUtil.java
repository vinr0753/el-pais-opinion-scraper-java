package com.browserstack.assignment.selenium_el_pais_scraper;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for downloading images from a given URL.
 *
 * Usage notes: - The folder path is created if missing. - The file name is
 * sanitized and query parameters removed. - If the file already exists, it is
 * not re-downloaded.
 */
public class DownloadUtil {

	private static final Logger logger = LoggerFactory.getLogger(DownloadUtil.class);

	/**
	 * Download image from 'url' into 'folderPath' and return the saved path, or
	 * null on failure.
	 */
	public static String downloadImage(String url, String folderPath) {
		// Validate input
		if (url == null || url.isBlank()) {
			logger.debug("[DownloadUtil] No image URL provided, skipping.");
			return null;
		}

		try {
			// Ensure the output directory exists
			Path folder = Path.of(folderPath);
			Files.createDirectories(folder);

			// Simplified filename extraction (strip query params)
			String fileName = url.substring(url.lastIndexOf('/') + 1).split("\\?")[0];
			fileName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");

			Path filePath = folder.resolve(fileName);

			// Skip download if file already exists
			if (Files.exists(filePath)) {
				logger.debug("[DownloadUtil] Image already exists: {}", filePath);
				return filePath.toString();
			}

			// Download and save the image
			try (InputStream in = new URL(url).openStream()) {
				Files.copy(in, filePath);
			}

			return filePath.toString();

		} catch (Exception e) {
			logger.warn("[DownloadUtil] Failed to download image: {}", e.getMessage());
			return null;
		}
	}
}
