package kroviq.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlexiField {

    // -- Identity --
    private String fieldKey;
    private String fieldLabel;
    private FlexiFieldType fieldType;
    private String blockName;
    private String sectionName;
    private String pageName;
    private String blockLocator;

    // -- Grouped Config --
    private CoreConfig coreConfig;
    private MrvGrouping mrvGrouping;
    private BusinessRules businessRules;
    private Advanced advanced;
    private SelectTypes selectTypes;
    private ApplicableChannels applicableChannels;

    // -- Runtime State --
    private boolean editable;
    private boolean visible;
    private String currentValue;

    private FlexiField() {}

    // -- Identity Getters --
    public String getFieldKey()          { return fieldKey; }
    public String getFieldLabel()        { return fieldLabel; }
    public FlexiFieldType getFieldType() { return fieldType; }
    public String getBlockName()         { return blockName; }
    public String getSectionName()       { return sectionName; }
    public String getPageName()          { return pageName; }
    public String getBlockLocator()      { return blockLocator; }

    // -- Grouped Config Getters --
    public CoreConfig getCoreConfig()               { return coreConfig; }
    public MrvGrouping getMrvGrouping()              { return mrvGrouping; }
    public BusinessRules getBusinessRules()          { return businessRules; }
    public Advanced getAdvanced()                    { return advanced; }
    public SelectTypes getSelectTypes()              { return selectTypes; }
    public ApplicableChannels getApplicableChannels(){ return applicableChannels; }

    // -- Runtime State Getters/Setters --
    public boolean isEditable()        { return editable; }
    public boolean isVisible()         { return visible; }
    public String getCurrentValue()    { return currentValue; }

    public void setCurrentValue(String currentValue) { this.currentValue = currentValue; }
    public void setEditable(boolean editable)        { this.editable = editable; }
    public void setVisible(boolean visible)          { this.visible = visible; }
    public void setFieldType(FlexiFieldType fieldType) { this.fieldType = fieldType; }
    public void setFieldLabel(String fieldLabel)     { this.fieldLabel = fieldLabel; }

    // -- Convenience: defaultValue shortcut --
    public String getDefaultValue() {
        return businessRules != null ? businessRules.getDefaultValue() : null;
    }

    public String identity() {
        boolean hasKey = fieldKey != null && !fieldKey.isEmpty();
        boolean hasLabel = fieldLabel != null && !fieldLabel.isEmpty();
        if (hasKey && hasLabel) return "'" + fieldLabel + "' (" + fieldKey + ")";
        if (hasKey) return "(" + fieldKey + ")";
        if (hasLabel) return "'" + fieldLabel + "'";
        return "(unknown)";
    }

    @Override
    public String toString() {
        return "FlexiField{" + identity() + ", type=" + fieldType + ", block='" + blockName + "'}";
    }

    // ============================================
    // Inner Classes -- Logical Grouping
    // ============================================

    public static class CoreConfig {
        private final String fieldName;
        private final String sortOrder;
        private final String lovType;
        private final String fieldSource;
        private final String parentLov;
        private final String configLabel;
        private final String toolTip;
        private final String minimumValue;
        private final String maximumValue;
        private final String dateRangeFrom;
        private final String dateRangeTo;

        private CoreConfig(CoreConfigBuilder b) {
            this.fieldName = b.fieldName;
            this.sortOrder = b.sortOrder;
            this.lovType = b.lovType;
            this.fieldSource = b.fieldSource;
            this.parentLov = b.parentLov;
            this.configLabel = b.configLabel;
            this.toolTip = b.toolTip;
            this.minimumValue = b.minimumValue;
            this.maximumValue = b.maximumValue;
            this.dateRangeFrom = b.dateRangeFrom;
            this.dateRangeTo = b.dateRangeTo;
        }

        public String getFieldName()    { return fieldName; }
        public String getSortOrder()    { return sortOrder; }
        public String getLovType()      { return lovType; }
        public String getFieldSource()  { return fieldSource; }
        public String getParentLov()    { return parentLov; }
        public String getConfigLabel()  { return configLabel; }
        public String getToolTip()      { return toolTip; }
        public String getMinimumValue() { return minimumValue; }
        public String getMaximumValue() { return maximumValue; }
        public String getDateRangeFrom(){ return dateRangeFrom; }
        public String getDateRangeTo()  { return dateRangeTo; }

        public static CoreConfigBuilder builder() { return new CoreConfigBuilder(); }

        public static class CoreConfigBuilder {
            private String fieldName, sortOrder, lovType, fieldSource, parentLov;
            private String configLabel, toolTip, minimumValue, maximumValue;
            private String dateRangeFrom, dateRangeTo;

            public CoreConfigBuilder fieldName(String v)    { this.fieldName = v; return this; }
            public CoreConfigBuilder sortOrder(String v)    { this.sortOrder = v; return this; }
            public CoreConfigBuilder lovType(String v)      { this.lovType = v; return this; }
            public CoreConfigBuilder fieldSource(String v)  { this.fieldSource = v; return this; }
            public CoreConfigBuilder parentLov(String v)    { this.parentLov = v; return this; }
            public CoreConfigBuilder configLabel(String v)  { this.configLabel = v; return this; }
            public CoreConfigBuilder toolTip(String v)      { this.toolTip = v; return this; }
            public CoreConfigBuilder minimumValue(String v) { this.minimumValue = v; return this; }
            public CoreConfigBuilder maximumValue(String v) { this.maximumValue = v; return this; }
            public CoreConfigBuilder dateRangeFrom(String v){ this.dateRangeFrom = v; return this; }
            public CoreConfigBuilder dateRangeTo(String v)  { this.dateRangeTo = v; return this; }
            public CoreConfig build() { return new CoreConfig(this); }
        }
    }

    public static class MrvGrouping {
        private final Boolean mrv;
        private final String mrvFieldOrder;
        private final String groupName;
        private final String groupOrderNum;

        private MrvGrouping(MrvGroupingBuilder b) {
            this.mrv = b.mrv;
            this.mrvFieldOrder = b.mrvFieldOrder;
            this.groupName = b.groupName;
            this.groupOrderNum = b.groupOrderNum;
        }

        public Boolean getMrv()            { return mrv; }
        public String getMrvFieldOrder()   { return mrvFieldOrder; }
        public String getGroupName()       { return groupName; }
        public String getGroupOrderNum()   { return groupOrderNum; }

        public static MrvGroupingBuilder builder() { return new MrvGroupingBuilder(); }

        public static class MrvGroupingBuilder {
            private Boolean mrv;
            private String mrvFieldOrder, groupName, groupOrderNum;

            public MrvGroupingBuilder mrv(Boolean v)            { this.mrv = v; return this; }
            public MrvGroupingBuilder mrvFieldOrder(String v)   { this.mrvFieldOrder = v; return this; }
            public MrvGroupingBuilder groupName(String v)       { this.groupName = v; return this; }
            public MrvGroupingBuilder groupOrderNum(String v)   { this.groupOrderNum = v; return this; }
            public MrvGrouping build() { return new MrvGrouping(this); }
        }
    }

    public static class BusinessRules {
        private final String tariffRange;
        private final String raApplicable;
        private final String mandatoryLevels;
        private final String defaultValue;

        private BusinessRules(BusinessRulesBuilder b) {
            this.tariffRange = b.tariffRange;
            this.raApplicable = b.raApplicable;
            this.mandatoryLevels = b.mandatoryLevels;
            this.defaultValue = b.defaultValue;
        }

        public String getTariffRange()    { return tariffRange; }
        public String getRaApplicable()   { return raApplicable; }
        public String getMandatoryLevels(){ return mandatoryLevels; }
        public String getDefaultValue()   { return defaultValue; }

        public static BusinessRulesBuilder builder() { return new BusinessRulesBuilder(); }

        public static class BusinessRulesBuilder {
            private String tariffRange, raApplicable, mandatoryLevels, defaultValue;

            public BusinessRulesBuilder tariffRange(String v)    { this.tariffRange = v; return this; }
            public BusinessRulesBuilder raApplicable(String v)   { this.raApplicable = v; return this; }
            public BusinessRulesBuilder mandatoryLevels(String v){ this.mandatoryLevels = v; return this; }
            public BusinessRulesBuilder defaultValue(String v)   { this.defaultValue = v; return this; }
            public BusinessRules build() { return new BusinessRules(this); }
        }
    }

    public static class Advanced {
        private final Boolean apiDriven;
        private final String events;
        private final String dependsOn;

        private Advanced(AdvancedBuilder b) {
            this.apiDriven = b.apiDriven;
            this.events = b.events;
            this.dependsOn = b.dependsOn;
        }

        public Boolean getApiDriven() { return apiDriven; }
        public String getEvents()     { return events; }
        public String getDependsOn()  { return dependsOn; }

        public static AdvancedBuilder builder() { return new AdvancedBuilder(); }

        public static class AdvancedBuilder {
            private Boolean apiDriven;
            private String events, dependsOn;

            public AdvancedBuilder apiDriven(Boolean v) { this.apiDriven = v; return this; }
            public AdvancedBuilder events(String v)     { this.events = v; return this; }
            public AdvancedBuilder dependsOn(String v)  { this.dependsOn = v; return this; }
            public Advanced build() { return new Advanced(this); }
        }
    }

    public static class SelectTypes {
        private final Boolean mandatory;
        private final Boolean enterable;
        private final Boolean hide;
        private final Boolean claimsLookup;
        private final Boolean tariff;
        private final Boolean excludeForTax;
        private final Boolean sectionSummary;
        private final Boolean uwListing;
        private final Boolean claimListing;
        private final Boolean userAuthorization;
        private final Boolean endorsementChanges;

        private SelectTypes(SelectTypesBuilder b) {
            this.mandatory = b.mandatory;
            this.enterable = b.enterable;
            this.hide = b.hide;
            this.claimsLookup = b.claimsLookup;
            this.tariff = b.tariff;
            this.excludeForTax = b.excludeForTax;
            this.sectionSummary = b.sectionSummary;
            this.uwListing = b.uwListing;
            this.claimListing = b.claimListing;
            this.userAuthorization = b.userAuthorization;
            this.endorsementChanges = b.endorsementChanges;
        }

        public Boolean getMandatory()          { return mandatory; }
        public Boolean getEnterable()          { return enterable; }
        public Boolean getHide()               { return hide; }
        public Boolean getClaimsLookup()       { return claimsLookup; }
        public Boolean getTariff()             { return tariff; }
        public Boolean getExcludeForTax()      { return excludeForTax; }
        public Boolean getSectionSummary()     { return sectionSummary; }
        public Boolean getUwListing()          { return uwListing; }
        public Boolean getClaimListing()       { return claimListing; }
        public Boolean getUserAuthorization()  { return userAuthorization; }
        public Boolean getEndorsementChanges() { return endorsementChanges; }

        public static SelectTypesBuilder builder() { return new SelectTypesBuilder(); }

        public static class SelectTypesBuilder {
            private Boolean mandatory, enterable, hide, claimsLookup, tariff;
            private Boolean excludeForTax, sectionSummary, uwListing, claimListing;
            private Boolean userAuthorization, endorsementChanges;

            public SelectTypesBuilder mandatory(Boolean v)          { this.mandatory = v; return this; }
            public SelectTypesBuilder enterable(Boolean v)          { this.enterable = v; return this; }
            public SelectTypesBuilder hide(Boolean v)               { this.hide = v; return this; }
            public SelectTypesBuilder claimsLookup(Boolean v)       { this.claimsLookup = v; return this; }
            public SelectTypesBuilder tariff(Boolean v)             { this.tariff = v; return this; }
            public SelectTypesBuilder excludeForTax(Boolean v)      { this.excludeForTax = v; return this; }
            public SelectTypesBuilder sectionSummary(Boolean v)     { this.sectionSummary = v; return this; }
            public SelectTypesBuilder uwListing(Boolean v)          { this.uwListing = v; return this; }
            public SelectTypesBuilder claimListing(Boolean v)       { this.claimListing = v; return this; }
            public SelectTypesBuilder userAuthorization(Boolean v)  { this.userAuthorization = v; return this; }
            public SelectTypesBuilder endorsementChanges(Boolean v) { this.endorsementChanges = v; return this; }
            public SelectTypes build() { return new SelectTypes(this); }
        }
    }

    public static class ApplicableChannels {
        private final List<String> channels;

        private ApplicableChannels(List<String> channels) {
            this.channels = channels != null
                    ? Collections.unmodifiableList(new ArrayList<>(channels))
                    : Collections.emptyList();
        }

        public List<String> getChannels() { return channels; }

        public static ApplicableChannels of(List<String> channels) {
            return new ApplicableChannels(channels);
        }
    }

    // ============================================
    // Builder
    // ============================================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final FlexiField field = new FlexiField();

        // Identity
        public Builder fieldKey(String val)          { field.fieldKey = val; return this; }
        public Builder fieldLabel(String val)        { field.fieldLabel = val; return this; }
        public Builder fieldType(FlexiFieldType val) { field.fieldType = val; return this; }
        public Builder blockName(String val)         { field.blockName = val; return this; }
        public Builder sectionName(String val)       { field.sectionName = val; return this; }
        public Builder pageName(String val)          { field.pageName = val; return this; }
        public Builder blockLocator(String val)      { field.blockLocator = val; return this; }

        // Grouped config
        public Builder coreConfig(CoreConfig val)                    { field.coreConfig = val; return this; }
        public Builder mrvGrouping(MrvGrouping val)                  { field.mrvGrouping = val; return this; }
        public Builder businessRules(BusinessRules val)              { field.businessRules = val; return this; }
        public Builder advanced(Advanced val)                        { field.advanced = val; return this; }
        public Builder selectTypes(SelectTypes val)                  { field.selectTypes = val; return this; }
        public Builder applicableChannels(ApplicableChannels val)    { field.applicableChannels = val; return this; }

        // Runtime state
        public Builder editable(boolean val)         { field.editable = val; return this; }
        public Builder visible(boolean val)          { field.visible = val; return this; }
        public Builder currentValue(String val)      { field.currentValue = val; return this; }

        public FlexiField build() {
            boolean hasKey = field.fieldKey != null && !field.fieldKey.isEmpty();
            boolean hasLabel = field.fieldLabel != null && !field.fieldLabel.isEmpty();
            if (!hasKey && !hasLabel) {
                throw new IllegalStateException(
                        "FlexiField requires at least a fieldKey or fieldLabel");
            }
            if (field.fieldType == null) {
                field.fieldType = FlexiFieldType.UNKNOWN;
            }
            return field;
        }
    }
}
