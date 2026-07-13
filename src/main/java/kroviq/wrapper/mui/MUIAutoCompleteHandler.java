package kroviq.wrapper.mui;

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

public class MUIAutoCompleteHandler implements AutoCompleteHandler {

    private static final Duration TIMEOUT = HandlerUtils.getTimeoutDuration("mui");
    private static final String POPPER_SELECTOR = ".MuiAutocomplete-popper, [role='listbox']";

    @Override
    public void searchAndSelect(WebDriver driver, WebElement input, String searchText, String optionToSelect) {
        HandlerUtils.clearAndType(driver, input, searchText);
        WebDriverWait wait = new WebDriverWait(driver, TIMEOUT);

        // Wait for autocomplete popper/listbox to appear
        WebElement listbox = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(POPPER_SELECTOR)));

        // Wait for options to be populated (not loading)
        wait.until(d -> !listbox.findElements(By.cssSelector("[role='option']")).isEmpty());

        WebElement option = HandlerUtils.findOptionByText(listbox, "[role='option']", optionToSelect, true);
        if (option == null) {
            throw new org.openqa.selenium.NoSuchElementException(
                    "MUI autocomplete option '" + optionToSelect + "' not found for search '" + searchText + "'");
        }

        option.click();
    }

    @Override
    public List<String> getSuggestions(WebDriver driver, WebElement input, String searchText) {
        HandlerUtils.clearAndType(driver, input, searchText);
        WebDriverWait wait = new WebDriverWait(driver, TIMEOUT);

        WebElement listbox = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(POPPER_SELECTOR)));
        wait.until(d -> !listbox.findElements(By.cssSelector("[role='option']")).isEmpty());

        List<WebElement> options = listbox.findElements(By.cssSelector("[role='option']"));
        List<String> texts = options.stream()
                .map(opt -> opt.getText().trim())
                .collect(Collectors.toList());

        // Close without selecting
        input.sendKeys(Keys.ESCAPE);
        return texts;
    }


}
