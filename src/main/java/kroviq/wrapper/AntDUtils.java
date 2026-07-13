package kroviq.wrapper;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.List;
import java.util.Set;

public class AntDUtils {
	private static final int MAX_SELECTION_RETRIES = 2;
	private final WebDriver driver;
	private final WebDriverWait wait;
	private final Actions actions;
	private final int timeoutSec;

	public AntDUtils(WebDriver driver, int timeoutSec) {
		this.driver = driver;
		this.timeoutSec = timeoutSec;
		this.wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSec));
		this.actions = new Actions(driver);
	}

	/**
	 * Wait for a dropdown panel to be fully open, visible, stable, and populated.
	 * Condition: panel is displayed AND not closing/hidden AND has >= 1 option.
	 * Uses the instance timeout (configurable via constructor).
	 */
	private WebElement waitForDropdownPanelReady() {
		return new WebDriverWait(driver, Duration.ofSeconds(timeoutSec)).until(d -> {
			List<WebElement> panels = d.findElements(By.cssSelector(".ant-select-dropdown"));
			for (WebElement p : panels) {
				try {
					if (!p.isDisplayed()) continue;
					if (isClosingPanel(p) || isHiddenPanel(p)) continue;
					String ariaHidden = p.getAttribute("aria-hidden");
					if ("true".equals(ariaHidden)) continue;
					String opacity = p.getCssValue("opacity");
					if ("0".equals(opacity)) continue;
					if (!p.findElements(By.cssSelector(".ant-select-item-option")).isEmpty()) {
						return p;
					}
				} catch (StaleElementReferenceException e) { /* re-poll */ }
				  catch (Exception e) { /* try next */ }
			}
			return null;
		});
	}

	/**
	 * Returns the active AntD dropdown panel using trigger-panel binding.
	 * PRIMARY: aria-controls (fast + reliable).
	 * FALLBACK: waitForDropdownPanelReady() (condition-based).
	 * Skips ensureAllPanelsClosed() when trigger is provided (already clicked externally).
	 */
	private WebElement activePanel(WebElement triggerElement) {
		// Only close stale panels when no trigger was provided (legacy callers)
		if (triggerElement == null) {
			ensureAllPanelsClosed();
		}

		// PRIMARY: Try aria-controls binding
		if (triggerElement != null) {
			String ariaControls = null;
			try {
				ariaControls = triggerElement.getAttribute("aria-controls");
			} catch (Exception e) { /* ignore */ }

			if (ariaControls != null && !ariaControls.trim().isEmpty()) {
				System.out.println("[AntDUtils] Using aria-controls binding: " + ariaControls);
				try {
					WebElement panel = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(ariaControls)));
					if (panel.isDisplayed() && !isClosingPanel(panel) && !isHiddenPanel(panel)) {
						System.out.println("[AntDUtils] Found panel via aria-controls: id=" + ariaControls);
						return panel;
					}
					System.out.println("[AntDUtils] aria-controls panel is closing/hidden, falling back");
				} catch (Exception e) {
					System.out.println("[AntDUtils] aria-controls panel not found, falling back");
				}
			} else {
				System.out.println("[AntDUtils] No aria-controls attribute, using panel-ready fallback");
			}
		}

		// FALLBACK: condition-based wait for any ready panel
		System.out.println("[AntDUtils] Waiting for dropdown panel to be ready...");
		return waitForDropdownPanelReady();
	}
	
	/**
	 * Capture current panel IDs in the DOM.
	 */
	private Set<String> getCurrentPanelIds() {
		Set<String> ids = new java.util.HashSet<>();
		String[] selectors = {".ant-select-dropdown", "[role='listbox']", ".rc-virtual-list"};
		
		for (String selector : selectors) {
			List<WebElement> panels = driver.findElements(By.cssSelector(selector));
			for (WebElement p : panels) {
				try {
					if (p.isDisplayed()) {
						String id = p.getAttribute("id");
						if (id != null && !id.isEmpty()) {
							ids.add(id);
						} else {
							// Use element reference as fallback
							ids.add("ref_" + System.identityHashCode(p));
						}
					}
				} catch (Exception e) { /* ignore */ }
			}
		}
		return ids;
	}
	
	/**
	 * Detect the newly opened panel by comparing before/after panel states.
	 */
	private WebElement detectNewlyOpenedPanel(Set<String> panelIdsBefore) {
		System.out.println("[AntDUtils] Detecting newly opened panel...");
		
		// Wait for a new stable panel to appear
		return wait.until(d -> {
			// Only look for .ant-select-dropdown as primary selector
			List<WebElement> panels = d.findElements(By.cssSelector(".ant-select-dropdown"));
			
			for (WebElement p : panels) {
				try {
					// Skip if not displayed, closing, or hidden
					if (!p.isDisplayed() || isClosingPanel(p) || isHiddenPanel(p)) continue;
					
					// Check if this is a new panel
					String id = p.getAttribute("id");
					String panelKey = (id != null && !id.isEmpty()) ? id : "ref_" + System.identityHashCode(p);
					
					if (!panelIdsBefore.contains(panelKey)) {
						// Verify it has options
						List<WebElement> options = p.findElements(By.cssSelector(".ant-select-item-option, [role='option']"));
						if (!options.isEmpty()) {
							System.out.println("[AntDUtils] Found newly opened panel: id=" + id);
							return p;
						}
					}
				} catch (Exception e) { /* try next */ }
			}
			return null;
		});
	}


	/**
	 * Check if a panel is in closing animation state.
	 * Returns true if panel has 'leave' or 'leave-active' classes.
	 */
	private boolean isClosingPanel(WebElement panel) {
		try {
			String className = panel.getAttribute("class");
			if (className != null) {
				return className.contains("leave") || className.contains("leave-active");
			}
		} catch (Exception e) {
			// If we can't read the class, assume it's not closing
		}
		return false;
	}
	
	/**
	 * Check if a panel is hidden via class or style.
	 * AntD hides panels using:
	 *   - class "ant-select-dropdown-hidden"
	 *   - style "pointer-events: none"
	 *   - style "display: none" / "visibility: hidden"
	 */
	private boolean isHiddenPanel(WebElement panel) {
		try {
			String className = panel.getAttribute("class");
			if (className != null && className.contains("ant-select-dropdown-hidden")) {
				return true;
			}
		} catch (Exception e) { /* ignore */ }
		try {
			String style = panel.getAttribute("style");
			if (style != null) {
				return style.contains("display: none") || style.contains("display:none") ||
				       style.contains("visibility: hidden") || style.contains("visibility:hidden") ||
				       style.contains("pointer-events: none") || style.contains("pointer-events:none");
			}
		} catch (Exception e) { /* ignore */ }
		return false;
	}

	/**
	 * Find option with virtualized scroll support.
	 * Scrolls the dropdown container if option is not immediately visible.
	 */
	private String findOptionWithVirtualScroll(WebElement panel, String visibleText) {
		// First check if option is already rendered
		List<WebElement> options = panel.findElements(By.cssSelector(".ant-select-item-option"));
		System.out.println("[AntDUtils] Found " + options.size() + " option(s) initially rendered");
		
		for (WebElement opt : options) {
			String optText = extractOptionText(opt);
			if (optText.equals(visibleText) || optText.equalsIgnoreCase(visibleText)) {
				System.out.println("[AntDUtils] Matched option: '" + optText + "'");
				return optText;
			}
		}
		
		// Option not found, try scrolling if virtualized
		WebElement scrollContainer = findScrollContainer(panel);
		if (scrollContainer != null) {
			System.out.println("[AntDUtils] Option not found, scrolling virtualized list...");
			return scrollToFindOption(panel, scrollContainer, visibleText);
		}
		
		// Not virtualized or option not found
		return null;
	}
	
	/**
	 * Find the scrollable container within the dropdown panel.
	 */
	private WebElement findScrollContainer(WebElement panel) {
		String[] scrollSelectors = {
			".rc-virtual-list-holder",
			".rc-virtual-list",
			".ant-select-dropdown .rc-virtual-list-holder",
			"div[class*='virtual-list']"
		};
		
		for (String selector : scrollSelectors) {
			try {
				List<WebElement> containers = panel.findElements(By.cssSelector(selector));
				for (WebElement container : containers) {
					if (container.isDisplayed()) {
						// Check if scrollable
						Object result = ((JavascriptExecutor) driver).executeScript(
							"return arguments[0].scrollHeight > arguments[0].clientHeight;", container);
						if (Boolean.TRUE.equals(result)) {
							System.out.println("[AntDUtils] Found scroll container: " + selector);
							return container;
						}
					}
				}
			} catch (Exception e) { /* try next */ }
		}
		return null;
	}
	
	/**
	 * Scroll the container to find the option.
	 */
	private String scrollToFindOption(WebElement panel, WebElement scrollContainer, String visibleText) {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		
		// Get scroll dimensions
		Number scrollHeightNum = (Number) js.executeScript("return arguments[0].scrollHeight;", scrollContainer);
		Number clientHeightNum = (Number) js.executeScript("return arguments[0].clientHeight;", scrollContainer);
		long scrollHeight = scrollHeightNum.longValue();
		long clientHeight = clientHeightNum.longValue();
		long scrollStep = clientHeight / 2; // Scroll half viewport at a time
		long currentScroll = 0;
		
		while (currentScroll < scrollHeight) {
			// Scroll down
			currentScroll += scrollStep;
			js.executeScript("arguments[0].scrollTop = arguments[1];", scrollContainer, currentScroll);
			
			// Wait for new options to render
			try {
				new WebDriverWait(driver, Duration.ofMillis(100)).until(d -> true);
			} catch (Exception e) { /* ignore */ }
			
			// Check if option appeared
			List<WebElement> options = panel.findElements(By.cssSelector(".ant-select-item-option"));
			for (WebElement opt : options) {
				String optText = extractOptionText(opt);
				if (optText.equals(visibleText) || optText.equalsIgnoreCase(visibleText)) {
					System.out.println("[AntDUtils] Found option after scrolling: '" + optText + "'");
					return optText;
				}
			}
			
			// Check if reached end
			Number actualScrollNum = (Number) js.executeScript("return arguments[0].scrollTop;", scrollContainer);
			long actualScroll = actualScrollNum.longValue();
			if (actualScroll + clientHeight >= scrollHeight) {
				System.out.println("[AntDUtils] Reached end of list, option not found");
				break;
			}
		}
		
		return null;
	}

	/**
	 * Wait for dropdown option to be fully interactable in virtualized lists.
	 * Confirms element is visible, enabled, and has stable bounding box.
	 */
	private void waitForOptionInteractable(WebElement panel, String optionText) {
		WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(3));
		shortWait.until(d -> {
			try {
				// Re-query option to avoid stale element
				WebElement option = findOptionByText(panel, optionText);
				
				if (!option.isDisplayed() || !option.isEnabled()) return false;
				
				// Check stable bounding box (height > 0)
				org.openqa.selenium.Rectangle rect = option.getRect();
				if (rect.getHeight() <= 0 || rect.getWidth() <= 0) return false;
				
				// Verify not obscured by checking if clickable point exists
				Object result = ((JavascriptExecutor) driver).executeScript(
					"var rect = arguments[0].getBoundingClientRect();" +
					"return rect.width > 0 && rect.height > 0;",
					option
				);
				return Boolean.TRUE.equals(result);
			} catch (Exception e) {
				return false;
			}
		});
	}

	/** Type into search box if present (showSearch mode). */
	private void tryTypeFilter(String query) {
		List<WebElement> ins = driver.findElements(By.cssSelector(".ant-select-selection-search-input"));
		for (WebElement in : ins) {
			if (in.isDisplayed()) {
				in.clear();
				in.sendKeys(query);
				new WebDriverWait(driver, Duration.ofMillis(250)).until(d -> true);
				return;
			}
		}
	}

	/** Page-down scan for virtualized lists until an option appears. */
	public void scanFor(By optBy, WebElement panel) {
		WebElement scroller = firstDisplayed(panel,
				By.cssSelector(".rc-virtual-list-holder, .rc-virtual-list, .ant-select-dropdown"));
		for (int i = 0; i < 15 && panel.findElements(optBy).isEmpty(); i++) {
			actions.moveToElement(scroller).sendKeys(Keys.PAGE_DOWN).perform();
			new WebDriverWait(driver, Duration.ofMillis(120)).until(d -> true);
		}
	}

	private WebElement firstDisplayed(WebElement scope, By by) {
		for (WebElement e : scope.findElements(by))
			if (e.isDisplayed())
				return e;
		return scope;
	}

	private static String x(String s) { // escape for XPath
		if (!s.contains("'"))
			return "'" + s + "'";
		if (!s.contains("\""))
			return "\"" + s + "\"";
		String[] p = s.split("'");
		StringBuilder b = new StringBuilder("concat(");
		for (int i = 0; i < p.length; i++) {
			b.append("'").append(p[i]).append("'");
			if (i < p.length - 1)
				b.append(", \"'\", ");
		}
		b.append(")");
		return b.toString();
	}

	private void jsClick(WebElement el) {
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
		((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
	}



	/** Ensure all dropdown panels are closed before proceeding */
	private void ensureAllPanelsClosed() {
		try {
			// Only check .ant-select-dropdown panels
			List<WebElement> panels = driver.findElements(By.cssSelector(".ant-select-dropdown"));
			
			// Quick check: any visible panels?
			boolean anyVisible = false;
			for (WebElement p : panels) {
				try {
					if (p.isDisplayed() && !isClosingPanel(p) && !isHiddenPanel(p)) {
						anyVisible = true;
						System.out.println("[AntDUtils] Found visible panel, waiting for close...");
						break;
					}
				} catch (Exception ex) { /* ignore */ }
			}
			
			// If no visible panels, return immediately
			if (!anyVisible) {
				System.out.println("[AntDUtils] No visible panels, proceeding");
				return;
			}
			
			// If panels exist, wait max 300ms (increased from 50ms)
			try {
				new WebDriverWait(driver, Duration.ofMillis(300)).until(d -> {
					List<WebElement> currentPanels = d.findElements(By.cssSelector(".ant-select-dropdown"));
					for (WebElement p : currentPanels) {
						try {
							if (p.isDisplayed() && !isClosingPanel(p) && !isHiddenPanel(p)) {
								return false; // Still visible
							}
						} catch (Exception ex) { /* ignore */ }
					}
					return true; // All closed
				});
				System.out.println("[AntDUtils] All panels closed successfully");
			} catch (TimeoutException e) {
				System.out.println("[AntDUtils] Panel close timeout, forcing close with ESC");
				actions.sendKeys(Keys.ESCAPE).perform();
				// Condition-based wait for ESC to take effect
				try {
					new WebDriverWait(driver, Duration.ofMillis(300)).until(d -> {
						List<WebElement> remaining = d.findElements(By.cssSelector(".ant-select-dropdown"));
						return remaining.stream().noneMatch(p -> {
							try { return p.isDisplayed() && !isClosingPanel(p); } catch (Exception ex) { return false; }
						});
					});
				} catch (TimeoutException te) { /* best-effort */ }
			}
		} catch (Exception e) {
			System.out.println("[AntDUtils] Panel cleanup error: " + e.getMessage());
		}
	}

	/** Finalize dropdown selection - ensure panel closed and state stabilized */
	private void finalizeDropdownSelection() {
		// Only check .ant-select-dropdown panels
		List<WebElement> panels = driver.findElements(By.cssSelector(".ant-select-dropdown"));
		
		// Quick check: is panel already closed?
		boolean anyVisible = false;
		for (WebElement p : panels) {
			try {
				if (p.isDisplayed() && !isClosingPanel(p) && !isHiddenPanel(p)) {
					anyVisible = true;
					break;
				}
			} catch (Exception ex) { /* ignore */ }
		}
		
		// If already closed, return immediately
		if (!anyVisible) {
			System.out.println("[AntDUtils] Dropdown panel already closed");
			return;
		}
		
		// If still visible, wait max 300ms (increased from 50ms)
		try {
			new WebDriverWait(driver, Duration.ofMillis(300)).until(d -> {
				List<WebElement> currentPanels = d.findElements(By.cssSelector(".ant-select-dropdown"));
				for (WebElement p : currentPanels) {
					try {
						if (p.isDisplayed() && !isClosingPanel(p) && !isHiddenPanel(p)) {
							return false; // Still visible
						}
					} catch (Exception ex) { /* ignore */ }
				}
				return true; // All closed
			});
			System.out.println("[AntDUtils] Dropdown panel closed successfully");
		} catch (TimeoutException e) {
			System.out.println("[AntDUtils] Panel still visible after selection, but proceeding");
			// Don't force close here - let it close naturally
		}
	}

	
	/**
	 * Extract visible text from AntD option element.
	 * Tries multiple strategies to handle different AntD versions.
	 */
	private String extractOptionText(WebElement option) {
		// Strategy 1: Try .ant-select-item-option-content (most common)
		try {
			WebElement content = option.findElement(By.cssSelector(".ant-select-item-option-content"));
			String text = content.getText().trim();
			if (!text.isEmpty()) {
				return text;
			}
		} catch (Exception e) { /* try next */ }
		
		// Strategy 2: Try direct getText() on option (fallback)
		try {
			String text = option.getText().trim();
			if (!text.isEmpty()) {
				return text;
			}
		} catch (Exception e) { /* try next */ }
		
		// Strategy 3: Try getAttribute("textContent") (last resort)
		try {
			String text = option.getAttribute("textContent");
			if (text != null) {
				return text.trim();
			}
		} catch (Exception e) { /* ignore */ }
		
		return ""; // No text found
	}

	/**
	 * Find option element by exact text match.
	 * Re-queries the panel to avoid stale element references.
	 */
	private WebElement findOptionByText(WebElement panel, String text) {
		List<WebElement> options = panel.findElements(By.cssSelector(".ant-select-item-option"));
		for (WebElement opt : options) {
			String optText = extractOptionText(opt);
			if (optText.equals(text) || optText.equalsIgnoreCase(text)) {
				return opt;
			}
		}
		throw new NoSuchElementException("Option with text '" + text + "' not found during re-resolution");
	}

	/**
	 * Attempt to find, wait-for-interactable, and click an option within a panel.
	 * Retries up to MAX_SELECTION_RETRIES on StaleElementException,
	 * ElementClickInterceptedException, or NoSuchElementException only.
	 */
	/**
	 * Scroll the matched option into the virtual list viewport so it is rendered and interactable.
	 */
	private void scrollOptionIntoView(WebElement panel, String matchedText) {
		try {
			WebElement option = findOptionByText(panel, matchedText);
			((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", option);
		} catch (NoSuchElementException | StaleElementReferenceException e) {
			// Element not currently rendered — let the retry loop handle it
		}
	}

	private void selectOptionWithRetry(WebElement panel, String matchedText, String elementName) {
		for (int attempt = 1; attempt <= MAX_SELECTION_RETRIES; attempt++) {
			try {
				scrollOptionIntoView(panel, matchedText);
				waitForOptionInteractable(panel, matchedText);
				WebElement optionToClick = findOptionByText(panel, matchedText);
				jsClick(optionToClick);
				return; // success
			} catch (StaleElementReferenceException | ElementClickInterceptedException | NoSuchElementException e) {
				System.out.println("[Dropdown] Retrying selection (attempt " + attempt + ") for element " + elementName + ": " + e.getClass().getSimpleName());
				if (attempt == MAX_SELECTION_RETRIES) {
					throw e;
				}
			}
		}
	}

	/**
	 * Select an AntD option by its visible text (handles portals + virtualization).
	 * Fail-fast approach with optional debug diagnostics.
	 */
	public void selectAntDOptionByText(String visibleText, String elementName, WebElement triggerElement) throws InterruptedException {
	    System.out.println("[AntDUtils] selectAntDOptionByText called with value: '" + visibleText + "'");
	    WebElement panel = activePanel(triggerElement);
	    System.out.println("[AntDUtils] Active panel found: tag=" + panel.getTagName() + ", class=" + panel.getAttribute("class"));

	    // Wait until any options are present
	    wait.until(d -> !panel.findElements(By.cssSelector(".ant-select-item-option")).isEmpty());

	    // Try to find option with virtualized scroll support
	    String matchedText = findOptionWithVirtualScroll(panel, visibleText);

	    if (matchedText == null) {
	        System.err.println("Dropdown value '" + visibleText + "' not present in options for '" + elementName + "'");
	        List<WebElement> allOptions = panel.findElements(By.cssSelector(".ant-select-item-option"));
	        logDropdownDiagnostics(visibleText, allOptions);
	        kroviq.reporting.StepReportingWrapper.recordManualStep(
	            "Dropdown value '" + visibleText + "' not present in options for '" + elementName + "'",
	            "FAIL"
	        );
	        throw new AssertionError("Dropdown value '" + visibleText + "' not found in '" + elementName + "'");
	    }

	    // Retry-protected selection step
	    selectOptionWithRetry(panel, matchedText, elementName);
	    System.out.println("[AntDUtils] Option clicked successfully");

	    // Finalize selection
	    finalizeDropdownSelection();
	}
	
	/** Backward compatibility overloads */
	public void selectAntDOptionByText(String visibleText, String elementName) throws InterruptedException {
	    selectAntDOptionByText(visibleText, elementName, null);
	}
	
	public void selectAntDOptionByText(String visibleText) throws InterruptedException {
	    selectAntDOptionByText(visibleText, "UNKNOWN_ELEMENT", null);
	}
	// in AntDUtils.java
	public void selectAntDOptionByTextClickOnly(String text) {
	    String dropdown = "//div[contains(@class,'ant-select-dropdown') and not(contains(@class,'hidden'))]";
	    String opt = dropdown + "//div[contains(@class,'ant-select-item-option')]"
	        + "[.//div[contains(@class,'ant-select-item-option-content') and normalize-space()='" + text + "']]";
	    WebElement el = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(opt)));
	    ((JavascriptExecutor)driver).executeScript("arguments[0].click();", el);
	}
	
	/** Click-only option select (no ENTER/ESC); safe inside forms/modals. */
	public void selectAntDOptionByTextNoKeys(String visibleText, String elementName, WebElement triggerElement) {
	    WebElement panel = activePanel(triggerElement);

	    // Wait until any options appear
	    wait.until(d -> !panel.findElements(By.cssSelector(".ant-select-item-option")).isEmpty());

	    // Try to find option with virtualized scroll support
	    String matchedText = findOptionWithVirtualScroll(panel, visibleText);

	    if (matchedText == null) {
	        System.err.println("Dropdown value '" + visibleText + "' not present in options for '" + elementName + "'");
	        List<WebElement> allOptions = panel.findElements(By.cssSelector(".ant-select-item-option"));
	        logDropdownDiagnostics(visibleText, allOptions);
	        kroviq.reporting.StepReportingWrapper.recordManualStep(
	            "Dropdown value '" + visibleText + "' not present in options for '" + elementName + "'",
	            "FAIL"
	        );
	        throw new AssertionError("Dropdown value '" + visibleText + "' not found in '" + elementName + "'");
	    }

	    // Retry-protected selection step
	    selectOptionWithRetry(panel, matchedText, elementName);

	    // Finalize selection
	    finalizeDropdownSelection();
	}
	
	/** Backward compatibility overloads */
	public void selectAntDOptionByTextNoKeys(String visibleText, String elementName) {
	    selectAntDOptionByTextNoKeys(visibleText, elementName, null);
	}
	
	public void selectAntDOptionByTextNoKeys(String visibleText) {
	    selectAntDOptionByTextNoKeys(visibleText, "UNKNOWN_ELEMENT", null);
	}

	/**
	 * Select an option from an already-open dropdown panel.
	 * Does NOT reopen or call activePanel(). Does NOT call finalizeDropdownSelection().
	 * Validates panel is still visible before selecting.
	 * Used by GenericActionHandler for multi-select iterations where the panel must stay open.
	 */
	public void selectOptionFromOpenPanel(WebElement panel, String visibleText, String elementName) throws InterruptedException {
	    System.out.println("[AntDUtils] selectOptionFromOpenPanel called with value: '" + visibleText + "'");

	    // Validate panel is still visible and ready (do NOT reopen)
	    waitForDropdownPanelReady();

	    // Wait until any options are present
	    wait.until(d -> !panel.findElements(By.cssSelector(".ant-select-item-option")).isEmpty());

	    // Try to find option with virtualized scroll support
	    String matchedText = findOptionWithVirtualScroll(panel, visibleText);

	    if (matchedText == null) {
	        System.err.println("Dropdown value '" + visibleText + "' not present in options for '" + elementName + "'");
	        List<WebElement> allOptions = panel.findElements(By.cssSelector(".ant-select-item-option"));
	        logDropdownDiagnostics(visibleText, allOptions);
	        kroviq.reporting.StepReportingWrapper.recordManualStep(
	            "Dropdown value '" + visibleText + "' not present in options for '" + elementName + "'",
	            "FAIL"
	        );
	        throw new AssertionError("Dropdown value '" + visibleText + "' not found in '" + elementName + "'");
	    }

	    // Retry-protected selection step
	    selectOptionWithRetry(panel, matchedText, elementName);
	    System.out.println("[AntDUtils] Option clicked (panel kept open)");
	}

	/**
	 * Find the currently visible dropdown panel without closing any panels.
	 * Used by GenericActionHandler to get a panel reference for multi-select.
	 */
	public WebElement findVisiblePanel() {
	    return wait.until(d -> {
	        List<WebElement> panels = d.findElements(By.cssSelector(".ant-select-dropdown"));
	        for (WebElement p : panels) {
	            try {
	                if (p.isDisplayed() && !isClosingPanel(p) && !isHiddenPanel(p)) {
	                    List<WebElement> options = p.findElements(By.cssSelector(".ant-select-item-option, [role='option']"));
	                    if (!options.isEmpty()) return p;
	                }
	            } catch (Exception e) { /* try next */ }
	        }
	        return null;
	    });
	}

	/**
	 * Log dropdown diagnostics only on failure.
	 * Helps debug test failures without slowing down successful runs.
	 */
	private void logDropdownDiagnostics(String expectedValue, List<WebElement> allOptions) {
		System.err.println("\n========== DROPDOWN SELECTION FAILED ==========");
		System.err.println("Expected value: '" + expectedValue + "'");
		System.err.println("Total options found: " + allOptions.size());
		System.err.println("Available options:");
		for (int i = 0; i < Math.min(20, allOptions.size()); i++) {
			System.err.println("  [" + (i+1) + "] '" + extractOptionText(allOptions.get(i)) + "'");
		}
		if (allOptions.size() > 20) {
			System.err.println("  ... and " + (allOptions.size() - 20) + " more options");
		}
		System.err.println("===============================================\n");
	}

	/**
	 * Select AntD option with Event Name parsing for Rule Engine dropdown.
	 * ONLY MODIFICATION: Extracts text after "Event Name:" for comparison.
	 * All other behavior identical to selectAntDOptionByText.
	 */
	public void selectAntDOptionByTextWithEventNameParsing(String visibleText, String elementName, WebElement triggerElement) throws InterruptedException {
	    System.out.println("[AntDUtils] selectAntDOptionByTextWithEventNameParsing called with value: '" + visibleText + "'");
	    WebElement panel = activePanel(triggerElement);
	    System.out.println("[AntDUtils] Active panel found: tag=" + panel.getTagName() + ", class=" + panel.getAttribute("class"));

	    // Wait until any options are present
	    wait.until(d -> !panel.findElements(By.cssSelector(".ant-select-item-option")).isEmpty());

	    // Try to find option with Event Name parsing
	    String matchedText = findOptionWithEventNameParsing(panel, visibleText);

	    if (matchedText == null) {
	        System.err.println("Dropdown value '" + visibleText + "' not present in options for '" + elementName + "'");
	        
	        List<WebElement> allOptions = panel.findElements(By.cssSelector(".ant-select-item-option"));
	        logDropdownDiagnostics(visibleText, allOptions);
	        
	        kroviq.reporting.StepReportingWrapper.recordManualStep(
	            "Dropdown value '" + visibleText + "' not present in options for '" + elementName + "'",
	            "FAIL"
	        );
	        
	        throw new AssertionError("Dropdown value '" + visibleText + "' not found in '" + elementName + "'");
	    }

	    // Wait for option to be interactable, then click
	    waitForOptionInteractable(panel, matchedText);
	    WebElement optionToClick = findOptionByEventName(panel, visibleText);
	    jsClick(optionToClick);
	    System.out.println("[AntDUtils] Option clicked successfully");

	    // Finalize selection
	    finalizeDropdownSelection();
	}

	/**
	 * Find option with Event Name parsing and virtualized scroll support.
	 * ONLY MODIFICATION: Extracts text after "Event Name:" for comparison.
	 */
	private String findOptionWithEventNameParsing(WebElement panel, String visibleText) {
		// First check if option is already rendered
		List<WebElement> options = panel.findElements(By.cssSelector(".ant-select-item-option"));
		System.out.println("[AntDUtils] Found " + options.size() + " option(s) initially rendered");
		
		for (WebElement opt : options) {
			String optText = extractOptionText(opt);
			String extractedValue = extractEventName(optText);
			if (extractedValue.equals(visibleText) || extractedValue.equalsIgnoreCase(visibleText)) {
				System.out.println("[AntDUtils] Matched option with Event Name: '" + extractedValue + "'");
				return optText; // Return full text for later re-resolution
			}
		}
		
		// Option not found, try scrolling if virtualized
		WebElement scrollContainer = findScrollContainer(panel);
		if (scrollContainer != null) {
			System.out.println("[AntDUtils] Option not found, scrolling virtualized list...");
			return scrollToFindOptionWithEventName(panel, scrollContainer, visibleText);
		}
		
		return null;
	}

	/**
	 * Scroll to find option with Event Name parsing.
	 */
	private String scrollToFindOptionWithEventName(WebElement panel, WebElement scrollContainer, String visibleText) {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		
		Number scrollHeightNum = (Number) js.executeScript("return arguments[0].scrollHeight;", scrollContainer);
		Number clientHeightNum = (Number) js.executeScript("return arguments[0].clientHeight;", scrollContainer);
		long scrollHeight = scrollHeightNum.longValue();
		long clientHeight = clientHeightNum.longValue();
		long scrollStep = clientHeight / 2;
		long currentScroll = 0;
		
		while (currentScroll < scrollHeight) {
			currentScroll += scrollStep;
			js.executeScript("arguments[0].scrollTop = arguments[1];", scrollContainer, currentScroll);
			
			try {
				new WebDriverWait(driver, Duration.ofMillis(100)).until(d -> true);
			} catch (Exception e) { /* ignore */ }
			
			List<WebElement> options = panel.findElements(By.cssSelector(".ant-select-item-option"));
			for (WebElement opt : options) {
				String optText = extractOptionText(opt);
				String extractedValue = extractEventName(optText);
				if (extractedValue.equals(visibleText) || extractedValue.equalsIgnoreCase(visibleText)) {
					System.out.println("[AntDUtils] Found option after scrolling with Event Name: '" + extractedValue + "'");
					return optText;
				}
			}
			
			Number actualScrollNum = (Number) js.executeScript("return arguments[0].scrollTop;", scrollContainer);
			long actualScroll = actualScrollNum.longValue();
			if (actualScroll + clientHeight >= scrollHeight) {
				System.out.println("[AntDUtils] Reached end of list, option not found");
				break;
			}
		}
		
		return null;
	}

	/**
	 * Extract Event Name from option text.
	 * If text contains "Event Name:", extract only the text after it (first line).
	 * Otherwise, return original text unchanged.
	 */
	private String extractEventName(String optionText) {
		if (optionText.contains("Event Name:")) {
			String[] lines = optionText.split("\\n");
			for (String line : lines) {
				if (line.trim().startsWith("Event Name:")) {
					return line.replace("Event Name:", "").trim();
				}
			}
		}
		return optionText; // Fallback to original text
	}

	/**
	 * Find option element by Event Name match.
	 */
	private WebElement findOptionByEventName(WebElement panel, String eventName) {
		List<WebElement> options = panel.findElements(By.cssSelector(".ant-select-item-option"));
		for (WebElement opt : options) {
			String optText = extractOptionText(opt);
			String extractedValue = extractEventName(optText);
			if (extractedValue.equals(eventName) || extractedValue.equalsIgnoreCase(eventName)) {
				return opt;
			}
		}
		throw new NoSuchElementException("Option with Event Name '" + eventName + "' not found during re-resolution");
	}

}