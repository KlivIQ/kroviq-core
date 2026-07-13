package kroviq.wrapper.desktop.core;

public class DesktopLocator {

    private final String strategy;
    private final String value;

    private DesktopLocator(String strategy, String value) {
        this.strategy = strategy;
        this.value = value;
    }

    public String strategy() { return strategy; }

    public String value() { return value; }

    public static DesktopLocator byAutomationId(String id) {
        return new DesktopLocator("automationId", id);
    }

    public static DesktopLocator byName(String name) {
        return new DesktopLocator("name", name);
    }

    public static DesktopLocator byClassName(String className) {
        return new DesktopLocator("className", className);
    }

    public static DesktopLocator byXPath(String xpath) {
        return new DesktopLocator("xpath", xpath);
    }

    public static DesktopLocator parse(String locatorString) {
        if (locatorString == null || locatorString.isEmpty()) {
            throw new IllegalArgumentException("Locator string cannot be null or empty");
        }
        int colonIndex = locatorString.indexOf(':');
        if (colonIndex <= 0 || colonIndex >= locatorString.length() - 1) {
            throw new IllegalArgumentException(
                "Invalid desktop locator format: '" + locatorString + "'. Expected 'strategy:value'");
        }
        String strategy = locatorString.substring(0, colonIndex).trim();
        String value = locatorString.substring(colonIndex + 1).trim();
        return switch (strategy) {
            case "automationId" -> byAutomationId(value);
            case "name" -> byName(value);
            case "className" -> byClassName(value);
            case "xpath" -> byXPath(value);
            default -> throw new IllegalArgumentException(
                "Unsupported desktop locator strategy: '" + strategy + "'. Supported: automationId, name, className, xpath");
        };
    }

    public static boolean isDesktopLocator(String locatorString) {
        if (locatorString == null || locatorString.isEmpty()) return false;
        int colonIndex = locatorString.indexOf(':');
        if (colonIndex <= 0) return false;
        String strategy = locatorString.substring(0, colonIndex).trim();
        return "automationId".equals(strategy) || "name".equals(strategy)
                || "className".equals(strategy) || "xpath".equals(strategy);
    }

    @Override
    public String toString() {
        return strategy + ":" + value;
    }
}
