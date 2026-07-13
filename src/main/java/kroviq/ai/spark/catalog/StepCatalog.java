package kroviq.ai.spark.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class StepCatalog {
    private final List<StepEntry> entries;

    public StepCatalog(List<StepEntry> entries) {
        this.entries = entries != null ? Collections.unmodifiableList(entries) : Collections.emptyList();
    }

    public int size() { return entries.size(); }

    public List<StepEntry> getAll() { return entries; }

    public List<StepEntry> getByCategory(StepCategory category) {
        return entries.stream()
                .filter(e -> e.getCategory() == category)
                .collect(Collectors.toList());
    }

    public List<StepEntry> getByKeyword(String keyword) {
        return entries.stream()
                .filter(e -> e.getKeyword().equalsIgnoreCase(keyword))
                .collect(Collectors.toList());
    }

    public Optional<StepMatch> findBestMatch(StepCategory category, String keyword, int paramCount) {
        List<StepEntry> candidates = entries.stream()
                .filter(e -> e.getCategory() == category)
                .filter(e -> e.getKeyword().equalsIgnoreCase(keyword))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            candidates = entries.stream()
                    .filter(e -> e.getCategory() == category)
                    .collect(Collectors.toList());
        }

        if (candidates.isEmpty()) return Optional.empty();

        // Exact param count match = 100% confidence
        for (StepEntry entry : candidates) {
            if (entry.getParams().size() == paramCount && entry.getKeyword().equalsIgnoreCase(keyword)) {
                return Optional.of(new StepMatch(entry, 100, entry.getExample()));
            }
        }

        // Category match + keyword match = 90%
        for (StepEntry entry : candidates) {
            if (entry.getKeyword().equalsIgnoreCase(keyword)) {
                return Optional.of(new StepMatch(entry, 90, entry.getExample()));
            }
        }

        // Category match only = 70%
        StepEntry fallback = candidates.get(0);
        return Optional.of(new StepMatch(fallback, 70, fallback.getExample()));
    }

    public Optional<StepMatch> findExactPattern(String pattern) {
        return entries.stream()
                .filter(e -> e.getPattern().equals(pattern))
                .findFirst()
                .map(e -> new StepMatch(e, 100, e.getExample()));
    }

    public static StepCatalog empty() {
        return new StepCatalog(Collections.emptyList());
    }
}
