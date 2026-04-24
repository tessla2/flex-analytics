package tessla2.FlexAnalytics.domain.service;

import com.opencsv.exceptions.CsvException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tessla2.FlexAnalytics.domain.model.ScenarioSummary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperimenterMergeServiceTest {

    private final ExperimenterMergeService service = new ExperimenterMergeService();

    @TempDir
    Path tempDir;

    @Test
    void shouldMergeAndSortScenariosByTotalThroughput() throws IOException, CsvException {
        Path fileA = tempDir.resolve("exp_a.csv");
        Path fileB = tempDir.resolve("exp_b.csv");
        Path output = tempDir.resolve("merged.csv");

        Files.writeString(fileA, "ScenarioID,RepNum,Object,Throughput\n" +
                "1,1,Saida Linha A,10\n" +
                "1,1,Saida Linha B,5\n" +
                "2,1,Saida Linha A,20\n" +
                "2,1,Saida Linha B,5\n");

        Files.writeString(fileB, "ScenarioID,RepNum,Object,Throughput\n" +
                "1,2,Saida Linha A,12\n" +
                "1,2,Saida Linha B,7\n" +
                "2,2,Saida Linha A,18\n" +
                "2,2,Saida Linha B,5\n");

        List<ScenarioSummary> summaries = service.mergeAndSummarize(
                List.of(fileA.toString(), fileB.toString()),
                output.toString(),
                "ScenarioID",
                "Object",
                "Throughput"
        );

        assertEquals(2, summaries.size());
        assertTrue(summaries.get(0).totalThroughput() >= summaries.get(1).totalThroughput());
        assertTrue(Files.exists(output));

        String exported = Files.readString(output);
        assertTrue(exported.contains("scenario_id"));
        assertTrue(exported.contains("total_throughput"));
    }

    @Test
    void shouldFailWhenRequiredColumnsAreMissing() throws IOException {
        Path invalid = tempDir.resolve("invalid.csv");
        Files.writeString(invalid, "Scenario,RepNum,Object,Throughput\n1,1,A,10\n");

        assertThrows(IllegalArgumentException.class,
                () -> service.mergeAndSummarize(
                        List.of(invalid.toString()),
                        tempDir.resolve("out.csv").toString(),
                        "ScenarioID",
                        "Object",
                        "Throughput"
                ));
    }
}
