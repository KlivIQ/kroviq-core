package kroviq.wrapper.aggrid;

public final class AgGridLocators {

    private AgGridLocators() {}

    // Grid structure
    public static final String ROOT_WRAPPER = ".ag-root-wrapper";
    public static final String BODY_VIEWPORT = ".ag-body-viewport";

    // Headers
    public static final String HEADER_CELL = ".ag-header-cell";
    public static final String HEADER_CELL_TEXT = ".ag-header-cell-text";

    // Rows and cells
    public static final String ROW = ".ag-row";
    public static final String CELL = ".ag-cell";

    // Row by index attribute
    public static String rowByIndex(int rowIndex) {
        return ".ag-row[row-index='" + rowIndex + "']";
    }

    // Cell by column index within a row
    public static String cellByColIndex(int colIndex) {
        return ".ag-cell[aria-colindex='" + (colIndex + 1) + "']";
    }

    // Detection markers
    public static final String[] DETECTION_MARKERS = {"ag-root", "ag-grid", "ag-body-viewport", "ag-row"};
}
