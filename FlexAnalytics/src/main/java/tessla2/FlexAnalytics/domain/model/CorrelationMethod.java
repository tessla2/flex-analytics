package tessla2.FlexAnalytics.domain.model;

public enum CorrelationMethod {
    PEARSON,
    SPEARMAN;

    public static CorrelationMethod from(String value, CorrelationMethod defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return CorrelationMethod.valueOf(value.trim().toUpperCase());
    }
}
