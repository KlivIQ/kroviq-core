package kroviq.ai.spark.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class StepCatalogLoader {

    private static final String DEFAULT_CATALOG_FILE = "step-catalog.json";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static StepCatalog load(Path catalogPath) {
        if (!Files.exists(catalogPath)) {
            System.out.println("[Spark] WARNING: step-catalog.json not found at " + catalogPath);
            System.out.println("[Spark] Generating with built-in Kroviq step vocabulary.");
            return loadBuiltIn();
        }

        try {
            String json = Files.readString(catalogPath);
            JsonNode root = mapper.readTree(json);
            List<StepEntry> entries = new ArrayList<>();

            for (JsonNode node : root) {
                entries.add(new StepEntry(
                        node.get("keyword").asText(),
                        node.get("pattern").asText(),
                        StepCategory.valueOf(node.get("category").asText()),
                        parseParams(node.get("params")),
                        node.has("source") ? node.get("source").asText() : "Unknown",
                        node.has("example") ? node.get("example").asText() : ""
                ));
            }

            System.out.println("[Spark] Step catalog loaded: " + entries.size() + " entries from " + catalogPath);
            return new StepCatalog(entries);
        } catch (IOException e) {
            System.out.println("[Spark] ERROR loading step-catalog.json: " + e.getMessage());
            return loadBuiltIn();
        }
    }

    public static StepCatalog loadFromProjectRoot(String projectRoot) {
        return load(Path.of(projectRoot, DEFAULT_CATALOG_FILE));
    }

    public static StepCatalog loadBuiltIn() {
        List<StepEntry> entries = new ArrayList<>();

        // Navigation
        entries.add(entry("When", "I open {string} from main menu", StepCategory.NAVIGATE,
                List.of("menuName"), "CommonSteps", "When I open \"ModuleName\" from main menu"));

        // Click
        entries.add(entry("When", "I click on {string} on {string} page", StepCategory.CLICK,
                List.of("element", "page"), "CommonSteps", "When I click on \"SAVE_BTN\" on \"LoginPage\" page"));
        entries.add(entry("When", "I click on {string} in {string} on {string} page", StepCategory.CLICK,
                List.of("element", "section", "page"), "CommonSteps", "When I click on \"ADD_BTN\" in \"Header\" on \"LoginPage\" page"));
        entries.add(entry("When", "I click on {string} in modal", StepCategory.CLICK,
                List.of("element"), "CommonSteps", "When I click on \"SAVE_BTN\" in modal"));
        entries.add(entry("When", "I click on {string}", StepCategory.CLICK,
                List.of("element"), "CommonSteps", "When I click on \"SAVE_BTN\""));

        // Input
        entries.add(entry("When", "I enter {string} in {string} on {string} page", StepCategory.INPUT,
                List.of("value", "element", "page"), "CommonSteps", "When I enter \"TD_Username\" in \"USERNAME_FIELD\" on \"LoginPage\" page"));
        entries.add(entry("When", "I enter {string} in {string} field on {string} page", StepCategory.INPUT,
                List.of("value", "element", "page"), "CommonSteps", "When I enter \"TD_Username\" in \"USERNAME\" field on \"LoginPage\" page"));

        // Select / Dropdown
        entries.add(entry("When", "I select {string} from {string} on {string} page", StepCategory.SELECT,
                List.of("value", "element", "page"), "CommonSteps", "When I select \"TD_Country\" from \"COUNTRY_DROPDOWN\" on \"LoginPage\" page"));
        entries.add(entry("When", "I select {string} from {string} in modal", StepCategory.SELECT,
                List.of("value", "element"), "CommonSteps", "When I select \"TD_Value\" from \"DROPDOWN\" in modal"));

        // Date
        entries.add(entry("When", "I set {string} date to {string} on {string} page", StepCategory.DATE,
                List.of("element", "value", "page"), "CommonSteps", "When I set \"START_DATE\" date to \"TD_StartDate\" on \"LoginPage\" page"));
        entries.add(entry("When", "I set {string} date to {string} in modal", StepCategory.DATE,
                List.of("element", "value"), "CommonSteps", "When I set \"DATE_FIELD\" date to \"TD_Date\" in modal"));

        // Toggle
        entries.add(entry("When", "I set {string} to {string} on {string} page", StepCategory.TOGGLE,
                List.of("element", "value", "page"), "CommonSteps", "When I set \"ACTIVE_TOGGLE\" to \"TD_ToggleValue\" on \"LoginPage\" page"));

        // Radio
        entries.add(entry("When", "I select radio option {string} from {string} on {string} page", StepCategory.RADIO,
                List.of("value", "element", "page"), "CommonSteps", "When I select radio option \"TD_Option\" from \"RADIO_GROUP\" on \"LoginPage\" page"));

        // Checkbox
        entries.add(entry("When", "I set {string} checkbox to {string} on {string} page", StepCategory.CHECKBOX,
                List.of("element", "value", "page"), "CommonSteps", "When I set \"AGREE_CHECKBOX\" checkbox to \"TD_Value\" on \"LoginPage\" page"));

        // Validate element state
        entries.add(entry("Then", "I verify element {string} is enabled on {string} page", StepCategory.VALIDATE_ELEMENT,
                List.of("element", "page"), "CommonSteps", "Then I verify element \"SAVE_BTN\" is enabled on \"LoginPage\" page"));
        entries.add(entry("Then", "I verify element {string} is disabled on {string} page", StepCategory.VALIDATE_ELEMENT,
                List.of("element", "page"), "CommonSteps", "Then I verify element \"SAVE_BTN\" is disabled on \"LoginPage\" page"));
        entries.add(entry("Then", "I verify {string} has value {string} and is {string} on {string} page", StepCategory.VALIDATE_ELEMENT,
                List.of("element", "value", "state", "page"), "CommonSteps", "Then I verify \"FIELD\" has value \"TD_Value\" and is \"disabled\" on \"LoginPage\" page"));
        entries.add(entry("Then", "I should see the {string} page", StepCategory.VALIDATE_ELEMENT,
                List.of("page"), "CommonSteps", "Then I should see the \"LoginPage\" page"));
        entries.add(entry("Then", "I should see the {string} on {string} page", StepCategory.VALIDATE_ELEMENT,
                List.of("section", "page"), "CommonSteps", "Then I should see the \"Header\" on \"LoginPage\" page"));

        // Validate message
        entries.add(entry("Then", "I validate message {string} contains {string} from {string} module on {string} page", StepCategory.VALIDATE_MESSAGE,
                List.of("element", "value", "module", "page"), "CommonSteps", "Then I validate message \"ERROR_MSG\" contains \"TD_Error\" from \"LoginErrorMessage\" module on \"LoginPage\" page"));
        entries.add(entry("Then", "I validate {string} outcome", StepCategory.VALIDATE_MESSAGE,
                List.of("outcome"), "CommonSteps", "Then I validate \"save\" outcome"));
        entries.add(entry("Then", "I verify toast message {string}", StepCategory.VALIDATE_MESSAGE,
                List.of("message"), "CommonSteps", "Then I verify toast message \"Record saved successfully\""));

        // Table operations
        entries.add(entry("Then", "I verify record with {string} = {string} exists in {string} table", StepCategory.VALIDATE_TABLE,
                List.of("column", "value", "table"), "CommonSteps", "Then I verify record with \"Name\" = \"TD_Name\" exists in \"listingTable\" table"));
        entries.add(entry("Then", "I verify first row in {string} table has {string} = {string}", StepCategory.VALIDATE_TABLE,
                List.of("table", "column", "value"), "CommonSteps", "Then I verify first row in \"listingTable\" table has \"Name\" = \"TD_Name\""));
        entries.add(entry("When", "I perform {string} action on record with {string} = {string} in {string} table", StepCategory.TABLE_ACTION,
                List.of("action", "column", "value", "table"), "CommonSteps", "When I perform \"Edit\" action on record with \"Name\" = \"TD_Name\" in \"listingTable\" table"));

        // Value capture
        entries.add(entry("Then", "I remember value of {string} on {string} page as {string}", StepCategory.VALUE_CAPTURE,
                List.of("element", "page", "alias"), "CommonSteps", "Then I remember value of \"QQ_NUMBER\" on \"LoginPage\" page as \"qqNumber\""));
        entries.add(entry("Then", "I verify {string} on {string} page equals remembered value {string}", StepCategory.VALUE_CAPTURE,
                List.of("element", "page", "alias"), "CommonSteps", "Then I verify \"QQ_NUMBER\" on \"LoginPage\" page equals remembered value \"qqNumber\""));

        // Wait
        entries.add(entry("Then", "I wait for {string} seconds", StepCategory.WAIT,
                List.of("seconds"), "CommonSteps", "Then I wait for \"3\" seconds"));

        System.out.println("[Spark] Built-in step catalog loaded: " + entries.size() + " entries");
        return new StepCatalog(entries);
    }

    private static StepEntry entry(String keyword, String pattern, StepCategory category,
                                   List<String> params, String source, String example) {
        return new StepEntry(keyword, pattern, category, params, source, example);
    }

    private static List<String> parseParams(JsonNode paramsNode) {
        List<String> params = new ArrayList<>();
        if (paramsNode != null && paramsNode.isArray()) {
            for (JsonNode p : paramsNode) {
                params.add(p.asText());
            }
        }
        return params;
    }
}
