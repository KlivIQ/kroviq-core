package kroviq.wrapper.factory;

import kroviq.wrapper.core.AutoCompleteHandler;
import kroviq.wrapper.core.DatePickerHandler;
import kroviq.wrapper.core.DialogHandler;
import kroviq.wrapper.core.DropdownHandler;
import kroviq.wrapper.core.FileUploadHandler;
import kroviq.wrapper.core.GridHandler;
import kroviq.wrapper.core.MultiSelectHandler;
import kroviq.wrapper.core.TableEngine;
import kroviq.wrapper.core.TreeHandler;
import kroviq.wrapper.core.DateTimeHandler;
import kroviq.wrapper.core.WizardHandler;
import kroviq.wrapper.core.RichTextHandler;
import kroviq.wrapper.core.CascaderHandler;
import kroviq.wrapper.aggrid.AgGridHandler;
import kroviq.wrapper.aggrid.AgGridTableEngine;
import kroviq.wrapper.angularmaterial.AngularMaterialAutoCompleteHandler;
import kroviq.wrapper.angularmaterial.AngularMaterialDatePickerHandler;
import kroviq.wrapper.angularmaterial.AngularMaterialDialogHandler;
import kroviq.wrapper.angularmaterial.AngularMaterialDropdownHandler;
import kroviq.wrapper.angularmaterial.AngularMaterialMultiSelectHandler;
import kroviq.wrapper.angularmaterial.AngularMaterialTableEngine;
import kroviq.wrapper.angularmaterial.AngularMaterialTableHandler;
import kroviq.wrapper.antd.AntDDatePickerHandler;
import kroviq.wrapper.antd.AntDDropdownAdapter;
import kroviq.wrapper.antd.AntDMultiSelectHandler;
import kroviq.wrapper.antd.AntDTableEngine;
import kroviq.wrapper.mui.MUIAutoCompleteHandler;
import kroviq.wrapper.mui.MUIDatePickerHandler;
import kroviq.wrapper.mui.MUIDialogHandler;
import kroviq.wrapper.mui.MUIDropdownHandler;
import kroviq.wrapper.mui.MUIMultiSelectHandler;
import kroviq.wrapper.mui.MUITableEngine;
import kroviq.wrapper.primeng.PrimeNGAutoCompleteHandler;
import kroviq.wrapper.primeng.PrimeNGDatePickerHandler;
import kroviq.wrapper.primeng.PrimeNGDropdownHandler;
import kroviq.wrapper.primeng.PrimeNGMultiSelectHandler;
import kroviq.wrapper.primeng.PrimeNGTableEngine;
import kroviq.wrapper.primeng.PrimeNGDialogHandler;
import kroviq.wrapper.primeng.PrimeNGDropdownHandler;
import kroviq.wrapper.primeng.PrimeNGTableHandler;
import kroviq.wrapper.factory.UIFrameworkDetector.UIFramework;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ComponentHandlerFactory {

    private static final Logger logger = LogManager.getLogger(ComponentHandlerFactory.class);

    private ComponentHandlerFactory() {}

    public static DropdownHandler getDropdownHandler(WebElement element, WebDriver driver) {
        UIFramework framework = UIFrameworkDetector.detect(element);
        return switch (framework) {
            case MUI -> new MUIDropdownHandler();
            case ANTD -> new AntDDropdownAdapter(driver);
            case ANGULAR_MATERIAL -> new AngularMaterialDropdownHandler();
            case PRIMENG -> new PrimeNGDropdownHandler();
            default -> new GenericDropdownHandler();
        };
    }

    public static AutoCompleteHandler getAutoCompleteHandler(WebElement element) {
        UIFramework framework = UIFrameworkDetector.detect(element);
        return switch (framework) {
            case MUI -> new MUIAutoCompleteHandler();
            case ANGULAR_MATERIAL -> new AngularMaterialAutoCompleteHandler();
            case PRIMENG -> new PrimeNGAutoCompleteHandler();
            default -> new MUIAutoCompleteHandler(); // Generic autocomplete follows MUI pattern (type + select)
        };
    }

    public static DialogHandler getDialogHandler(WebElement element) {
        UIFramework framework = UIFrameworkDetector.detect(element);
        return switch (framework) {
            case MUI -> new MUIDialogHandler();
            case ANGULAR_MATERIAL -> new AngularMaterialDialogHandler();
            case PRIMENG -> new PrimeNGDialogHandler();
            default -> new GenericDialogHandler();
        };
    }

    public static GridHandler getGridHandler(WebElement element, WebDriver driver) {
        UIFramework framework = UIFrameworkDetector.detect(element);
        return switch (framework) {
            case AG_GRID -> new AgGridHandler(element);
            case ANGULAR_MATERIAL -> new AngularMaterialTableHandler(element);
            case PRIMENG -> new PrimeNGTableHandler(element);
            default -> new GenericGridHandler(element);
        };
    }

    public static TableEngine getTableEngine(WebElement tableRoot, WebDriver driver) {
        UIFramework framework = UIFrameworkDetector.detect(tableRoot);
        logger.info("[TableEngine] Framework detected: {}", framework);
        return switch (framework) {
            case AG_GRID -> new AgGridTableEngine(tableRoot);
            case ANTD -> new AntDTableEngine(tableRoot, driver);
            case PRIMENG -> new PrimeNGTableEngine(tableRoot, driver);
            case ANGULAR_MATERIAL -> new AngularMaterialTableEngine(tableRoot, driver);
            case MUI -> new MUITableEngine(tableRoot, driver);
            default -> new GenericTableEngine(tableRoot);
        };
    }

    public static FileUploadHandler getFileUploadHandler(WebElement element, WebDriver driver) {
        return new GenericFileUploadHandler();
    }

    public static DatePickerHandler getDatePickerHandler(WebElement element, WebDriver driver) {
        UIFramework framework = UIFrameworkDetector.detect(element);
        logger.debug("[DatePicker] Framework detected: {}", framework);
        return switch (framework) {
            case ANTD -> new AntDDatePickerHandler(driver);
            case MUI -> new MUIDatePickerHandler(driver);
            case ANGULAR_MATERIAL -> new AngularMaterialDatePickerHandler(driver);
            case PRIMENG -> new PrimeNGDatePickerHandler(driver);
            default -> new GenericDatePickerHandler(driver);
        };
    }

    public static MultiSelectHandler getMultiSelectHandler(WebElement element, WebDriver driver) {
        UIFramework framework = UIFrameworkDetector.detect(element);
        logger.debug("[MultiSelect] Framework detected: {}", framework);
        return switch (framework) {
            case ANTD -> new AntDMultiSelectHandler(driver);
            case MUI -> new MUIMultiSelectHandler(driver);
            case ANGULAR_MATERIAL -> new AngularMaterialMultiSelectHandler(driver);
            case PRIMENG -> new PrimeNGMultiSelectHandler(driver);
            default -> new GenericMultiSelectHandler(driver);
        };
    }

    public static TreeHandler getTreeHandler(WebElement treeRoot, WebDriver driver) {
        return new GenericTreeHandler(driver);
    }

    public static DateTimeHandler getDateTimeHandler(WebElement element, WebDriver driver) {
        return new GenericDateTimeHandler(driver);
    }

    public static WizardHandler getWizardHandler(WebElement wizardRoot, WebDriver driver) {
        return new GenericWizardHandler(driver);
    }

    public static RichTextHandler getRichTextHandler(WebElement element, WebDriver driver) {
        return new GenericRichTextHandler();
    }

    public static CascaderHandler getCascaderHandler(WebElement element, WebDriver driver) {
        return new GenericCascaderHandler(driver);
    }
}
