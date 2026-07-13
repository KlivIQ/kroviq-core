package kroviq.hooks;

import io.cucumber.java.AfterStep;
import io.cucumber.java.BeforeStep;
import io.cucumber.java.Scenario;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import kroviq.reporting.*;
import kroviq.utils.TestContext;

public class StepHooks {
    private static final Logger logger = LogManager.getLogger(StepHooks.class);
    
    @BeforeStep
    public void beforeStep(Scenario scenario) {
        TestContext.setCurrentScenario(scenario);
        logger.debug("Before step execution for scenario: {}", scenario.getName());
    }
    
    @AfterStep
    public void afterStep(Scenario scenario) {
        try {
            logger.debug("After step execution for scenario: {}", scenario.getName());
        } catch (Exception e) {
            logger.error("Error in afterStep hook: {}", e.getMessage());
        }
    }
}
