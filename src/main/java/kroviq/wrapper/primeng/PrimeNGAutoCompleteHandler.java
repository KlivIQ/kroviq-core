package kroviq.wrapper.primeng;

import kroviq.wrapper.core.AutoCompleteHandler;
import kroviq.wrapper.core.HandlerUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class PrimeNGAutoCompleteHandler implements AutoCompleteHandler {

    private static final Duration TIMEOUT = HandlerUtils.getTimeoutDuration("primeng");
    private static final String OVERLAY_PANEL = ".p-autocomplete-panel, p-autocomplete-panel";
    private static final String OPTION_SELECTOR = ".p-autocomplete-item, li.p-autocomplete-item";
    private static final String INPUT_SELECTOR = "input.p-autocomplete-input, input.p-inputtext";

    @Override
    public void searchAndSelect(WebDriver driver, WebElement input, String searchText, String optionToSelect) {
        WebElement inputField = resolveInput(input);
        HandlerUtils.clearAndType(driver, inputField, searchText);

        WebDriverWait wait = new WebDriverWait(driver, TIMEOUT);
        WebElement panel = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(OVERLAY_PANEL)));
        wait.until(d -> !panel.findElements(By.cssSelector(OPTION_SELECTOR)).isEmpty());

        WebElement option = HandlerUtils.findOptionByText(panel, OPTION_SELECTOR, optionToSelect, true);
        if (option == null) {
            throw new org.openqa.selenium.NoSuchElementException(
                    "PrimeNG autocomplete option '" + optionToSelect + "' not found for search '" + searchText + "'");
        }
        option.click();
    }

    @Override
    public List<String> getSuggestions(WebDriver driver, WebElement input, String searchText) {
        WebElement inputField = resolveInput(input);
        HandlerUtils.clearAndType(driver, inputField, searchText);

        WebDriverWait wait = new WebDriverWait(driver, TIMEOUT);
        WebElement panel = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(OVERLAY_PANEL)));
        wait.until(d -> !panel.findElements(By.cssSelector(OPTION_SELECTOR)).isEmpty());

        List<WebElement> options = panel.findElements(By.cssSelector(OPTION_SELECTOR));
        List<String> texts = options.stream()
                .map(opt -> opt.getText().trim())
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toList());

        inputField.sendKeys(Keys.ESCAPE);
        return texts;
    }

    private WebElement resolveInput(WebElement element) {
        String tagName = element.getTagName().toLowerCase();
        if ("input".equals(tagName)) return element;
        List<WebElement> inputs = element.findElements(By.cssSelector(INPUT_SELECTOR));
        return inputs.isEmpty() ? element : inputs.get(0);
    }


}
