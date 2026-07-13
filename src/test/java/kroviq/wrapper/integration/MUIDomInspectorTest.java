package kroviq.wrapper.integration;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MUIDomInspectorTest {

    private static WebDriver driver;
    private static WebDriverWait wait;

    @BeforeAll
    static void setUp() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--window-size=1920,1080", "--disable-gpu");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @AfterAll
    static void tearDown() {
        if (driver != null) driver.quit();
    }

    @Test
    @Order(1)
    void inspectSelectPage() {
        driver.get("https://mui.com/material-ui/react-select/");
        waitForLoad();
        System.out.println("\n=== SELECT PAGE DOM INSPECTION ===");

        // Check for iframes
        List<WebElement> iframes = driver.findElements(By.tagName("iframe"));
        System.out.println("Iframes found: " + iframes.size());
        for (int i = 0; i < iframes.size(); i++) {
            System.out.println("  iframe[" + i + "] src=" + iframes.get(i).getAttribute("src"));
        }

        // Check for MUI select components directly on page
        List<WebElement> selects = driver.findElements(By.cssSelector("[class*='MuiSelect']"));
        System.out.println("MuiSelect elements: " + selects.size());

        List<WebElement> comboboxes = driver.findElements(By.cssSelector("[role='combobox']"));
        System.out.println("role=combobox elements: " + comboboxes.size());

        List<WebElement> formControls = driver.findElements(By.cssSelector("[class*='MuiFormControl']"));
        System.out.println("MuiFormControl elements: " + formControls.size());

        // Check for demo containers
        List<WebElement> demoRoots = driver.findElements(By.cssSelector("[class*='demo-root'], [class*='MuiBox-root']"));
        System.out.println("Demo root containers: " + demoRoots.size());

        // Try switching to first iframe if present
        if (!iframes.isEmpty()) {
            for (int i = 0; i < Math.min(3, iframes.size()); i++) {
                try {
                    driver.switchTo().frame(iframes.get(i));
                    List<WebElement> innerSelects = driver.findElements(By.cssSelector("[class*='MuiSelect'], [role='combobox']"));
                    System.out.println("  Inside iframe[" + i + "]: MuiSelect/combobox elements = " + innerSelects.size());
                    if (!innerSelects.isEmpty()) {
                        System.out.println("    First element class: " + innerSelects.get(0).getAttribute("class"));
                        System.out.println("    First element tag: " + innerSelects.get(0).getTagName());
                        System.out.println("    First element role: " + innerSelects.get(0).getAttribute("role"));
                    }
                    driver.switchTo().defaultContent();
                } catch (Exception e) {
                    System.out.println("  iframe[" + i + "] switch failed: " + e.getMessage());
                    driver.switchTo().defaultContent();
                }
            }
        }

        // If no iframes, dump first few MUI elements
        if (iframes.isEmpty() && !selects.isEmpty()) {
            for (int i = 0; i < Math.min(3, selects.size()); i++) {
                System.out.println("  MuiSelect[" + i + "] class=" + selects.get(i).getAttribute("class"));
                System.out.println("  MuiSelect[" + i + "] tag=" + selects.get(i).getTagName());
                System.out.println("  MuiSelect[" + i + "] text=" + selects.get(i).getText());
            }
        }

        // Look for the "Age" label demo specifically
        List<WebElement> labels = driver.findElements(By.xpath("//*[contains(text(),'Age')]"));
        System.out.println("Elements containing 'Age': " + labels.size());

        // Try page-level search for the demo select
        String pageSource = driver.getPageSource();
        System.out.println("Page contains 'BasicSelect': " + pageSource.contains("BasicSelect"));
        System.out.println("Page contains 'MuiSelect-select': " + pageSource.contains("MuiSelect-select"));
        System.out.println("Page contains 'role=\"combobox\"': " + pageSource.contains("role=\"combobox\""));
        System.out.println("Page contains 'iframe': " + pageSource.contains("<iframe"));
    }

    @Test
    @Order(2)
    void inspectAutocompletePage() {
        driver.get("https://mui.com/material-ui/react-autocomplete/");
        waitForLoad();
        System.out.println("\n=== AUTOCOMPLETE PAGE DOM INSPECTION ===");

        List<WebElement> iframes = driver.findElements(By.tagName("iframe"));
        System.out.println("Iframes found: " + iframes.size());

        List<WebElement> autocompletes = driver.findElements(By.cssSelector("[class*='MuiAutocomplete']"));
        System.out.println("MuiAutocomplete elements: " + autocompletes.size());

        List<WebElement> comboboxInputs = driver.findElements(By.cssSelector("input[role='combobox']"));
        System.out.println("input[role=combobox] elements: " + comboboxInputs.size());

        if (!iframes.isEmpty()) {
            for (int i = 0; i < Math.min(3, iframes.size()); i++) {
                try {
                    driver.switchTo().frame(iframes.get(i));
                    List<WebElement> inner = driver.findElements(By.cssSelector("[class*='MuiAutocomplete'], input[role='combobox']"));
                    System.out.println("  Inside iframe[" + i + "]: Autocomplete elements = " + inner.size());
                    if (!inner.isEmpty()) {
                        System.out.println("    First: class=" + inner.get(0).getAttribute("class") + " tag=" + inner.get(0).getTagName());
                    }
                    driver.switchTo().defaultContent();
                } catch (Exception e) {
                    System.out.println("  iframe[" + i + "] error: " + e.getMessage());
                    driver.switchTo().defaultContent();
                }
            }
        }

        if (autocompletes.isEmpty() && comboboxInputs.isEmpty()) {
            String src = driver.getPageSource();
            System.out.println("Page contains 'MuiAutocomplete': " + src.contains("MuiAutocomplete"));
            System.out.println("Page contains 'ComboBox': " + src.contains("ComboBox"));
        }
    }

    @Test
    @Order(3)
    void inspectDialogPage() {
        driver.get("https://mui.com/material-ui/react-dialog/");
        waitForLoad();
        System.out.println("\n=== DIALOG PAGE DOM INSPECTION ===");
        System.out.println("Current URL: " + driver.getCurrentUrl());
        System.out.println("Page title: " + driver.getTitle());

        List<WebElement> iframes = driver.findElements(By.tagName("iframe"));
        System.out.println("Iframes found: " + iframes.size());

        // Check all buttons on page
        List<WebElement> allButtons = driver.findElements(By.tagName("button"));
        System.out.println("Total buttons on page: " + allButtons.size());

        List<WebElement> buttons = driver.findElements(By.xpath("//button[contains(text(),'Open') or contains(text(),'OPEN')]"));
        System.out.println("Buttons with 'Open/OPEN' text: " + buttons.size());
        for (int i = 0; i < Math.min(5, buttons.size()); i++) {
            System.out.println("  button[" + i + "] text='" + buttons.get(i).getText() + "' displayed=" + buttons.get(i).isDisplayed());
        }

        // Check for MUI buttons specifically
        List<WebElement> muiButtons = driver.findElements(By.cssSelector("button[class*='MuiButton']"));
        System.out.println("MuiButton elements: " + muiButtons.size());
        for (int i = 0; i < Math.min(5, muiButtons.size()); i++) {
            System.out.println("  MuiButton[" + i + "] text='" + muiButtons.get(i).getText() + "' displayed=" + muiButtons.get(i).isDisplayed());
        }

        // Check page source for the button text
        String src = driver.getPageSource();
        System.out.println("Page contains 'OPEN ALERT DIALOG': " + src.contains("OPEN ALERT DIALOG"));
        System.out.println("Page contains 'Open alert dialog': " + src.contains("Open alert dialog"));
        System.out.println("Page contains 'AlertDialog': " + src.contains("AlertDialog"));

        // Try navigating to the specific anchor
        driver.get("https://mui.com/material-ui/react-dialog/#alerts");
        waitForLoad();
        System.out.println("\nAfter navigating to #alerts:");
        List<WebElement> alertButtons = driver.findElements(By.xpath("//button[contains(text(),'Open') or contains(text(),'OPEN')]"));
        System.out.println("Buttons with 'Open/OPEN': " + alertButtons.size());
        for (int i = 0; i < Math.min(5, alertButtons.size()); i++) {
            System.out.println("  button[" + i + "] text='" + alertButtons.get(i).getText() + "' displayed=" + alertButtons.get(i).isDisplayed());
        }
    }

    private void waitForLoad() {
        wait.until(d -> ((JavascriptExecutor) d).executeScript("return document.readyState").equals("complete"));
        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
    }
}
