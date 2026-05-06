package tessla2.FlexAnalytics.controller;

import com.opencsv.exceptions.CsvException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tessla2.FlexAnalytics.application.dto.*;
import tessla2.FlexAnalytics.application.mapper.SensitivityMapper;
import tessla2.FlexAnalytics.domain.model.DataSet;
import tessla2.FlexAnalytics.domain.model.SensitivityResult;
import tessla2.FlexAnalytics.domain.model.CorrelationMethod;
import tessla2.FlexAnalytics.domain.service.CsvReaderService;
import tessla2.FlexAnalytics.domain.service.ExperimenterDataService;
import tessla2.FlexAnalytics.domain.service.SensitivityService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/sensitivity")
public class SensitivityController {

    private final CsvReaderService csvReaderService;
    private final SensitivityService sensitivityService;
    private final SensitivityMapper sensitivityMapper;
    private final ExperimenterDataService experimenterDataService;

    @Value("${app.correlation-method:PEARSON}")
    private String defaultCorrelationMethod;

    @Value("${app.upload.max-file-size-bytes:10485760}")
    private long maxUploadFileSizeBytes;

    public SensitivityController(CsvReaderService csvReaderService,
                                 SensitivityService sensitivityService,
                                 SensitivityMapper sensitivityMapper,
                                 ExperimenterDataService experimenterDataService) {
        this.csvReaderService = csvReaderService;
        this.sensitivityService = sensitivityService;
        this.sensitivityMapper = sensitivityMapper;
        this.experimenterDataService = experimenterDataService;
    }

    @PostMapping("/complete-analysis")
    public CompleteAnalysisResponse completeAnalysis(
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "classpathFiles", required = false) List<String> classpathFiles,
            @RequestParam String outputColumn,
            @RequestParam(required = false) String correlationMethod,
            @RequestParam(required = false) String outputFile
    ) throws IOException, CsvException {

        List<String> allPaths = new ArrayList<>();
        List<Path> tempFiles = new ArrayList<>();

        if (files != null) {
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                Path tempFile = Files.createTempFile("flexsim-complete-", ".csv");
                file.transferTo(tempFile);
                tempFiles.add(tempFile);
                allPaths.add(tempFile.toString());
            }
        }

        if (classpathFiles != null) {
            for (String cpFile : classpathFiles) {
                allPaths.add("classpath:" + cpFile.trim());
            }
        }

        if (allPaths.isEmpty()) {
            throw new IllegalArgumentException("No files provided");
        }

        CorrelationMethod method = CorrelationMethod.from(correlationMethod, CorrelationMethod.PEARSON);

        try {
            Map<String, Map<String, Double>> aggregated = experimenterDataService.loadAndMergeAllNumeric(
                    allPaths, List.of("ScenarioID"));

            if (aggregated.isEmpty()) {
                throw new IllegalArgumentException("No data found in files");
            }

            List<String> inputColumns = new ArrayList<>();
            Set<String> firstRow = aggregated.values().iterator().next().keySet();
            for (String col : firstRow) {
                if (!col.equalsIgnoreCase(outputColumn) &&
                    !col.equalsIgnoreCase("ScenarioID") &&
                    !col.equalsIgnoreCase("RepNum") &&
                    !col.equalsIgnoreCase("Object")) {
                    inputColumns.add(col);
                }
            }

            DataSet dataSet = experimenterDataService.prepareForSensitivity(
                    aggregated, inputColumns, List.of(outputColumn));

            List<SensitivityResult> sensitivityResults = sensitivityService.analyze(dataSet, method);

            List<ScenarioRankingDTO> scenarioRanking = new ArrayList<>();
            List<Map.Entry<String, Map<String, Double>>> sortedScenarios = new ArrayList<>(aggregated.entrySet());
            sortedScenarios.sort((a, b) -> {
                Double outA = a.getValue().getOrDefault(outputColumn, 0.0);
                Double outB = b.getValue().getOrDefault(outputColumn, 0.0);
                return Double.compare(outB, outA);
            });

            int rank = 1;
            String bestScenarioId = null;
            for (Map.Entry<String, Map<String, Double>> entry : sortedScenarios) {
                Double output = entry.getValue().getOrDefault(outputColumn, 0.0);
                scenarioRanking.add(new ScenarioRankingDTO(entry.getKey(), output, rank++));
            }

            if (!sortedScenarios.isEmpty()) {
                bestScenarioId = sortedScenarios.get(0).getKey();
            }


            List<ResultDTO> sensitivityDTOs = sensitivityResults.stream()
                    .map(sensitivityMapper::toDto)
                    .toList();

            Map<String, Double> bestConfig = bestScenarioId != null ?
                    aggregated.get(bestScenarioId) : null;

            return new CompleteAnalysisResponse(
                    allPaths.toString(),
                    method.name(),
                    aggregated.size(),
                    inputColumns.size(),
                    sensitivityDTOs,
                    scenarioRanking,
                    bestScenarioId,
                    bestConfig
            );

        } finally {
            for (Path tempFile : tempFiles) {
                try { Files.deleteIfExists(tempFile); } catch (IOException ignored) {}
            }
        }
    }
}
