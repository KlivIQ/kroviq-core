package kroviq.utils;

public enum ActionType {
    CLICK(WaitHandler.WaitTier.SHORT),
    INPUT(WaitHandler.WaitTier.SHORT),
    DROPDOWN(WaitHandler.WaitTier.MEDIUM),
    DATE_PICKER(WaitHandler.WaitTier.MEDIUM),
    TOGGLE(WaitHandler.WaitTier.SHORT),
    VERIFICATION(WaitHandler.WaitTier.SHORT),
    NAVIGATION(WaitHandler.WaitTier.MEDIUM),
    UPLOAD(WaitHandler.WaitTier.LONG),
    COMPLEX(WaitHandler.WaitTier.LONG),
    UNKNOWN(WaitHandler.WaitTier.SHORT);
    
    private final WaitHandler.WaitTier defaultTier;
    
    ActionType(WaitHandler.WaitTier defaultTier) {
        this.defaultTier = defaultTier;
    }
    
    public WaitHandler.WaitTier getTier() {
        String configValue = LoadProperties.get("wait." + name().toLowerCase(), defaultTier.name().toLowerCase());
        try {
            return WaitHandler.WaitTier.valueOf(configValue.toUpperCase());
        } catch (Exception e) {
            return defaultTier;
        }
    }
}
