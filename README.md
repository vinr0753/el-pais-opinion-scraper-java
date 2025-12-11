# 📘 El País Opinion Scraper — Java + Selenium + BrowserStack

This project automates scraping articles from the **“Opinión”** section of *El País* using **Java, Selenium, TestNG**, and optionally executes cross-browser tests on **BrowserStack Automate**.

It extracts:

- Article titles  
- First paragraph  
- Article images (downloaded locally)  
- English translations (via **Google Translate API**)  
- Word-frequency analysis across translated titles  

The framework supports both **local execution** and **parallel BrowserStack execution**, uses rich logging, and automatically organizes downloaded files.

---

## 🚀 Features

### 🔹 Web Scraping
- Navigates to **https://elpais.com/opinion/**
- Extracts:
  - Article URLs  
  - Spanish article titles  
  - First paragraphs  
  - Main header images (downloaded locally)

### 🔹 Translation (Google Translate API)
- Converts titles **Spanish → English**
- Performs repeated word frequency analysis

### 🔹 Cross-Browser Testing (BrowserStack)
Runs parallel tests across:

| OS | Browser | Version |
|----|---------|---------|
| macOS Ventura | Chrome | 141 |
| Windows 10 | Chrome | 141 |
| Windows 11 | Chrome | latest |
| macOS Monterey | Chrome | latest |
| Samsung Galaxy S24 | Chrome | Android |

### 🔹 Local Run Mode
- Only **one** local browser session is allowed  
- Additional TestNG threads are **skipped automatically**  

### 🔹 Logging
- Clear logs for every scraper action  

---

## 🛠️ Tech Stack

| Component | Technology |
|----------|------------|
| Language | Java 17+ |
| Test Automation | Selenium WebDriver |
| Test Runner | TestNG |
| Build Tool | Maven |
| Logging | Java Util Logging |
| Cloud Grid | BrowserStack Automate |
| Translation API | Google Translate |
| Parallel Execution | Maven Surefire + TestNG |
