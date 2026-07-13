package kroviq.wrapper.core;

import org.openqa.selenium.WebDriver;

public interface TableEditor {

    void editCell(WebDriver driver, String rowColumn, String rowValue,
                  String targetColumn, String newValue);

    void editCellAs(WebDriver driver, String rowColumn, String rowValue,
                    String targetColumn, String newValue, CellEditType editType);

    boolean isCellEditable(WebDriver driver, String rowColumn, String rowValue,
                           String targetColumn);

    enum CellEditType {
        TEXT_INPUT,
        DROPDOWN,
        CHECKBOX,
        TOGGLE
    }
}
