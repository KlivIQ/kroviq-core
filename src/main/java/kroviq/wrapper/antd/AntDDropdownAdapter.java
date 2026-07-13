package kroviq.wrapper.antd;

import kroviq.wrapper.AntDUtils;
import kroviq.wrapper.core.DropdownHandler;
import kroviq.utils.LoadProperties;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import java.util.List;
import java.util.stream.Collectors;

public class AntDDropdownAdapter implements DropdownHandler {

    private final AntDUtils antDUtils;

    public AntDDropdownAdapter(WebDriver driver) {
        int timeout = Integer.parseInt(LoadProperties.get("explicitTimeOut", "15"));
        this.antDUtils = new AntDUtils(driver, timeout);
    }

    @Override
    public void select(WebDriver driver, WebElement trigger, String visibleText) {
        trigger.click();
        try {
            antDUtils.selectAntDOptionByText(visibleText, "dropdown", trigger);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("AntD dropdown selection interrupted", e);
        }
    }

    @Override
    public List<String> getOptions(WebDriver driver, WebElement trigger) {
        trigger.click();
        WebElement panel = antDUtils.findVisiblePanel();
        List<WebElement> options = panel.findElements(By.cssSelector(".ant-select-item-option"));
        return options.stream()
                .map(opt -> opt.getText().trim())
                .collect(Collectors.toList());
    }
}
