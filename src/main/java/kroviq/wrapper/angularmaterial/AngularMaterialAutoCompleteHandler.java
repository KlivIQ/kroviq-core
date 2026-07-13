package kroviq.wrapper.angularmaterial;

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

public class AngularMaterialAutoCompleteHandler implements AutoCompleteHandler {

    private static final Duration TIMEOUT = HandlerUtils.getTimeoutDuration("angularmaterial");
    private static final String AUTOCOMPLETE_PANEL = ".cdk-overlay-container .mat-mdc-autocomplete-panel, .cdk-overlay-container .mat-autocomplete-panel";
    private static final String OPTION_SELECTOR = "mat-option, .mat-mdc-option";

    @Override
    public void searchAndSelect(WebDriver driver, WebElement input, String searchText, String optionToSelect) {
        HandlerUtils.clearAndType(driver, input, searchText);
        WebDriverWait wait = new WebDriverWait(driver, TIMEOUT);

        WebElement panel = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(AUTOCOMPLETE_PANEL)));
        wait.until(d -> !panel.findElements(By.cssSelector(OPTION_SELECTOR)).isEmpty());

        WebElement option = HandlerUtils.findOptionByText(panel, OPTION_SELECTOR, optionToSelect, true);
        if (option == null) {
            throw new org.openqa.selenium.NoSuchElementException(
                    "Angular Material autocomplete option '" + optionToSelect + "' not found for search '" + searchText + "'");
        }
        option.click();
    }

    @Override
    public List<String> getSuggestions(WebDriver driver, WebElement input, String searchText) {
        HandlerUtils.clearAndType(driver, input, searchText);
        WebDriverWait wait = new WebDriverWait(driver, TIMEOUT);

        WebElement panel = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(AUTOCOMPLETE_PANEL)));
        wait.until(d -> !panel.findElements(By.cssSelector(OPTION_SELECTOR)).isEmpty());

        List<WebElement> options = panel.findElements(By.cssSelector(OPTION_SELECTOR));
        List<String> texts = options.stream()
                .map(opt -> opt.getText().trim())
                .collect(Collectors.toList());

        input.sendKeys(Keys.ESCAPE);
        return texts;
    }


}
