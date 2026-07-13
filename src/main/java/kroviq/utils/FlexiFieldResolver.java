package kroviq.utils;

import kroviq.model.FlexiField;
import kroviq.model.FlexiFieldType;
import kroviq.wrapper.GenericWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.List;

public class FlexiFieldResolver {

    private static final Logger logger = LogManager.getLogger(FlexiFieldResolver.class);

    // -- Element Resolution (block-scoped) --

    public static WebElement resolveElement(FlexiField field) {
        // Resolve block first -- all searches are scoped within it
        WebElement block = resolveBlock(field);
        WebElement element = resolveElementInBlock(field, block);
        return ensureStable(element);
    }

    private static WebElement resolveElementInBlock(FlexiField field, WebElement block) {
        // Primary: key-based (id starts with KeyXXX) within block
        String fieldKey = field.getFieldKey();
        if (fieldKey != null && !fieldKey.isEmpty()) {
            List<WebElement> matches = block.findElements(By.cssSelector("[id^='" + escapeCss(fieldKey) + "']"));
            if (!matches.isEmpty()) {
                if (matches.size() == 1) {
                    logger.debug("Resolved {} via key-based id^='{}' within block", field.identity(), fieldKey);
                    return matches.get(0);
                }
                throw new RuntimeException("Key-based resolution for '" + fieldKey
                        + "' matched " + matches.size() + " elements within block '" + field.getBlockName() + "' -- ambiguous.");
            }
        }

        // Secondary: label-based (normalized id contains normalized label) within block
        String label = field.getFieldLabel();
        if (label != null && !label.isEmpty()) {
            WebElement found = resolveByNormalizedLabelInBlock(block, label);
            if (found != null) {
                logger.debug("Resolved {} via label-based id match for '{}' within block", field.identity(), label);
                return found;
            }
        }

        throw new RuntimeException("Flexi field " + field.identity()
                + " not resolvable within block '" + field.getBlockName()
                + "' -- no element with id starting with '" + fieldKey
                + "' or containing '" + label + "'");
    }

    private static WebElement resolveByNormalizedLabelInBlock(WebElement block, String label) {
        // JS scoped to block container: normalize both IDs and label, check contains
        String script =
                "var norm = function(s) { return s.replace(/[\\s_\\-]/g, '').toLowerCase(); };" +
                "var target = norm(arguments[1]);" +
                "var all = arguments[0].querySelectorAll('[id]');" +
                "var hits = [];" +
                "for (var i = 0; i < all.length; i++) {" +
                "  if (norm(all[i].id).indexOf(target) !== -1) hits.push(all[i]);" +
                "}" +
                "return hits;";
        try {
            @SuppressWarnings("unchecked")
            List<WebElement> matches = (List<WebElement>) ((JavascriptExecutor) getDriver())
                    .executeScript(script, block, label);
            if (matches == null || matches.isEmpty()) return null;
            if (matches.size() == 1) return matches.get(0);
            throw new RuntimeException("Label-based resolution for '" + label
                    + "' matched " + matches.size() + " elements within block -- ambiguous.");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.debug("JS label resolution failed for '{}': {}", label, e.getMessage());
            return null;
        }
    }

    // -- Stability Check --

    private static WebElement ensureStable(WebElement element) {
        // Condition-based stability check: poll until element is not stale (max 2s)
        try {
            new WebDriverWait(getDriver(), Duration.ofSeconds(2))
                .ignoring(StaleElementReferenceException.class)
                .until(d -> {
                    element.getTagName(); // triggers staleness check
                    return element;
                });
        } catch (TimeoutException e) {
            throw new RuntimeException("Element became stale after 2s stability wait", e);
        }
        return element;
    }

    // -- Block Resolution (3-tier) --

    public static WebElement resolveBlock(FlexiField field) {
        return resolveBlock(field.getBlockName(), field.getBlockLocator());
    }

    public static WebElement resolveBlock(String blockName, String explicitLocator) {
        WebDriver driver = getDriver();

        // Tier 1: explicit locator override (if provided)
        if (explicitLocator != null && !explicitLocator.isEmpty()) {
            try {
                By locator = explicitLocator.startsWith("//") || explicitLocator.startsWith("(")
                        ? By.xpath(explicitLocator) : By.cssSelector(explicitLocator);
                WebElement block = driver.findElement(locator);
                if (block.isDisplayed()) {
                    logger.debug("Block '{}' resolved via explicit locator", blockName);
                    return block;
                }
            } catch (NoSuchElementException e) {
                logger.debug("Explicit locator failed for block '{}', trying header match", blockName);
            }
        }

        // Tier 2: exact header text match -> traverse to container
        WebElement block = resolveBlockByHeader(driver, blockName, true);
        if (block != null) {
            logger.debug("Block '{}' resolved via exact header match", blockName);
            return block;
        }

        // Tier 3: partial/contains header match -> traverse to container
        block = resolveBlockByHeader(driver, blockName, false);
        if (block != null) {
            logger.debug("Block '{}' resolved via partial header match", blockName);
            return block;
        }

        throw new RuntimeException("Block '" + blockName + "' not found on page");
    }

    private static WebElement resolveBlockByHeader(WebDriver driver, String blockName, boolean exact) {
        String condition = exact
                ? "normalize-space()='" + escapeXPath(blockName) + "'"
                : "contains(normalize-space(),'" + escapeXPath(blockName) + "')";

        // Search across common header tags
        String xpath = "//*[self::h5 or self::h4 or self::h3 or self::div[contains(@class,'group-header')] "
                + "or self::div[contains(@class,'block-header')] or self::div[contains(@class,'section-header')]]"
                + "[" + condition + "]";

        List<WebElement> headers = driver.findElements(By.xpath(xpath));
        for (WebElement header : headers) {
            if (!header.isDisplayed()) continue;
            WebElement container = traverseToBlockContainer(header);
            if (container != null) return container;
        }
        return null;
    }

    private static WebElement traverseToBlockContainer(WebElement header) {
        // Walk up to find the nearest block-level container
        String[] containerPatterns = {
                "ancestor::div[contains(@class,'ant-card')][1]",
                "ancestor::div[contains(@class,'block-container')][1]",
                "ancestor::div[contains(@class,'group-wrapper')][1]",
                "ancestor::div[contains(@class,'ant-collapse-item')][1]",
                "ancestor::div[contains(@class,'form-section')][1]",
                "ancestor::div[contains(@id,'_Container')][1]",
                ".."  // direct parent as last resort
        };
        for (String pattern : containerPatterns) {
            try {
                WebElement container = header.findElement(By.xpath(pattern));
                if (container != null && container.isDisplayed()) return container;
            } catch (NoSuchElementException e) { /* try next */ }
        }
        return null;
    }

    // -- Field Type Detection --

    public static FlexiFieldType detectFieldType(WebElement element) {
        // Walk up to find the component wrapper with the type marker class
        WebElement wrapper = findComponentWrapper(element);
        if (wrapper != null) {
            String cssClass = wrapper.getAttribute("class");
            FlexiFieldType detected = FlexiFieldType.fromCssClass(cssClass);
            if (detected != FlexiFieldType.UNKNOWN) return detected;
        }

        // Fallback: infer from element attributes
        return inferTypeFromElement(element);
    }

    private static WebElement findComponentWrapper(WebElement element) {
        try {
            return element.findElement(By.xpath(
                    "ancestor-or-self::div[contains(@class,'component-main-wrapper')][1]"));
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    private static FlexiFieldType inferTypeFromElement(WebElement element) {
        try {
            String tag = element.getTagName().toLowerCase();
            String type = element.getAttribute("type");
            String role = element.getAttribute("role");
            String cssClass = element.getAttribute("class");
            if (cssClass == null) cssClass = "";

            if ("combobox".equals(role) || cssClass.contains("ant-select")) return FlexiFieldType.DROPDOWN;
            if ("switch".equals(role) || cssClass.contains("ant-switch")) return FlexiFieldType.TOGGLE;
            if ("checkbox".equals(type) || cssClass.contains("ant-checkbox")) return FlexiFieldType.CHECKBOX;
            if ("radio".equals(type) || cssClass.contains("ant-radio")) return FlexiFieldType.RADIO;
            if ("number".equals(type) || cssClass.contains("ant-input-number")) return FlexiFieldType.NUMBER;
            if (cssClass.contains("ant-picker")) return FlexiFieldType.DATE_PICKER;
            if ("input".equals(tag) || "textarea".equals(tag)) return FlexiFieldType.TEXTBOX;
        } catch (Exception e) {
            logger.debug("Type inference failed: {}", e.getMessage());
        }
        return FlexiFieldType.UNKNOWN;
    }

    // -- Editable Detection (6 checks) --

    public static boolean isEditable(WebElement element) {
        try {
            // 1. disabled attribute
            if (element.getAttribute("disabled") != null) return false;

            // 2. readonly attribute
            if (element.getAttribute("readonly") != null) return false;

            // 3. aria-disabled
            if ("true".equals(element.getAttribute("aria-disabled"))) return false;

            // 4. wrapper disabled classes
            WebElement wrapper = findComponentWrapper(element);
            String wrapperClass = wrapper != null ? wrapper.getAttribute("class") : "";
            String elementClass = element.getAttribute("class");
            if (elementClass == null) elementClass = "";
            String combinedClass = wrapperClass + " " + elementClass;

            String[] disabledMarkers = {
                    "ant-select-disabled", "ant-input-disabled", "ant-picker-disabled",
                    "ant-switch-disabled", "ant-checkbox-disabled", "ant-radio-disabled",
                    "ant-input-number-disabled"
            };
            for (String marker : disabledMarkers) {
                if (combinedClass.contains(marker)) return false;
            }

            // 5. pointer-events: none (via JS)
            JavascriptExecutor js = (JavascriptExecutor) getDriver();
            String pointerEvents = (String) js.executeScript(
                    "var el = arguments[0];" +
                    "while (el && el !== document.body) {" +
                    "  if (window.getComputedStyle(el).pointerEvents === 'none') return 'none';" +
                    "  el = el.parentElement;" +
                    "}" +
                    "return 'auto';", element);
            if ("none".equals(pointerEvents)) return false;

            // 6. overlay/mask blocking
            Boolean blocked = (Boolean) js.executeScript(
                    "var rect = arguments[0].getBoundingClientRect();" +
                    "var cx = rect.left + rect.width / 2;" +
                    "var cy = rect.top + rect.height / 2;" +
                    "var top = document.elementFromPoint(cx, cy);" +
                    "if (!top) return true;" +
                    "return !arguments[0].contains(top) && !top.contains(arguments[0]);", element);
            if (Boolean.TRUE.equals(blocked)) return false;

            return true;
        } catch (Exception e) {
            logger.debug("Editable check failed for element, defaulting to false: {}", e.getMessage());
            return false;
        }
    }

    // -- Visibility Detection --

    public static boolean isVisible(WebElement element) {
        try {
            if (!element.isDisplayed()) return false;

            String style = element.getAttribute("style");
            if (style != null) {
                String lower = style.toLowerCase();
                if (lower.contains("display: none") || lower.contains("display:none")) return false;
                if (lower.contains("visibility: hidden") || lower.contains("visibility:hidden")) return false;
            }

            // Check if inside a collapsed section
            try {
                WebElement collapsed = element.findElement(
                        By.xpath("ancestor::div[contains(@class,'ant-collapse-content-hidden')][1]"));
                if (collapsed != null) return false;
            } catch (NoSuchElementException e) { /* not collapsed */ }

            return true;
        } catch (StaleElementReferenceException e) {
            return false;
        } catch (Exception e) {
            logger.debug("Visibility check failed: {}", e.getMessage());
            return false;
        }
    }

    // -- Helpers --

    private static String escapeCss(String value) {
        // Escape characters that are special in CSS attribute selectors
        StringBuilder sb = new StringBuilder();
        for (char c : value.toCharArray()) {
            if (c == '\'' || c == '\\' || c == '"') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static String escapeXPath(String value) {
        if (!value.contains("'")) return value;
        if (!value.contains("\"")) return value;
        // Use concat for values containing both quotes
        StringBuilder sb = new StringBuilder("concat(");
        String[] parts = value.split("'");
        for (int i = 0; i < parts.length; i++) {
            sb.append("'").append(parts[i]).append("'");
            if (i < parts.length - 1) sb.append(", \"'\", ");
        }
        sb.append(")");
        return sb.toString();
    }

    private static WebDriver getDriver() {
        WebDriver driver = GenericWrapper.getDriver();
        if (driver == null) {
            throw new RuntimeException("WebDriver is null -- cannot resolve flexi field elements");
        }
        return driver;
    }
}
