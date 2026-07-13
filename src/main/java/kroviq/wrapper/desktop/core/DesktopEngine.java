package kroviq.wrapper.desktop.core;

import kroviq.wrapper.desktop.session.DesktopSessionConfig;

public interface DesktopEngine {

    // Session
    void launchApplication(DesktopSessionConfig config);

    void attachToApplication(String windowTitle);

    void closeApplication();

    // Interaction
    void click(DesktopLocator locator);

    void doubleClick(DesktopLocator locator);

    void type(DesktopLocator locator, String text);

    void clear(DesktopLocator locator);

    String getText(DesktopLocator locator);

    boolean isVisible(DesktopLocator locator);

    boolean isEnabled(DesktopLocator locator);

    // Dropdown
    void selectDropdownValue(DesktopLocator locator, String value);

    // Checkbox/Toggle
    void setCheckbox(DesktopLocator locator, boolean checked);

    boolean isChecked(DesktopLocator locator);

    // Window
    String getCurrentWindowTitle();

    void switchToWindow(String title);
}
