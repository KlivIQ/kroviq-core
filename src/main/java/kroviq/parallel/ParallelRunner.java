package kroviq.parallel;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;
import kroviq.utils.RunManager;
import java.io.FileWriter;
import java.io.PrintWriter;

@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/resources/features",
    glue = {"kroviq.hooks", "stepdefinitions"},
    plugin = {
        "pretty",
        "html:target/parallel-reports",
        "json:target/parallel-reports/Cucumber.json",
        "junit:target/parallel-reports/Cucumber.xml"
    },
    monochrome = true
)
public class ParallelRunner {
    
    static {
        try {
            RunManager.initialize();
            
            String tagExpression = RunManager.getTagExpression();
            boolean parallelEnabled = RunManager.isParallelExecutionEnabled();
            int threads = RunManager.getThreadCount();
            
            try (PrintWriter writer = new PrintWriter(new FileWriter("cucumber.properties"))) {
                if (!tagExpression.isEmpty()) {
                    writer.println("cucumber.filter.tags=" + tagExpression);
                }
                writer.println("cucumber.execution.parallel.enabled=" + parallelEnabled);
                if (parallelEnabled) {
                    writer.println("cucumber.execution.parallel.config.fixed.parallelism=" + threads);
                }
            }
            
            System.out.println("cucumber.properties generated from RunManager.json");
        } catch (Exception e) {
            System.err.println("Error configuring ParallelRunner: " + e.getMessage());
        }
    }
}
