package kroviq.wrapper.desktop.table;

import java.util.Map;

public interface DesktopTable {

    DesktopRow findRow(String columnName, String value);

    DesktopRow findRowByCriteria(Map<String, String> criteria);

    boolean rowExists(String columnName, String value);

    String getCellValue(DesktopRow row, String columnName);

    int getRowCount();
}
