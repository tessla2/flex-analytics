package tessla2.FlexAnalytics.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CorrelationMethodTest {

    @Test
    void shouldUseDefaultWhenNullOrBlank() {
        assertEquals(CorrelationMethod.PEARSON, CorrelationMethod.from(null, CorrelationMethod.PEARSON));
        assertEquals(CorrelationMethod.SPEARMAN, CorrelationMethod.from("  ", CorrelationMethod.SPEARMAN));
    }

    @Test
    void shouldParseIgnoringCase() {
        assertEquals(CorrelationMethod.PEARSON, CorrelationMethod.from("pearson", CorrelationMethod.SPEARMAN));
        assertEquals(CorrelationMethod.SPEARMAN, CorrelationMethod.from("SpEaRmAn", CorrelationMethod.PEARSON));
    }

    @Test
    void shouldThrowWhenInvalidValue() {
        assertThrows(IllegalArgumentException.class,
                () -> CorrelationMethod.from("kendall", CorrelationMethod.PEARSON));
    }
}
