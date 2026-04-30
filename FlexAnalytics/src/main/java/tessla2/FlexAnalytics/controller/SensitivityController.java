package tessla2.FlexAnalytics.controller;

import com.opencsv.exceptions.CsvException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tessla2.FlexAnalytics.application.dto.*;
import tessla2.FlexAnalytics.application.mapper.ScenarioSummaryMapper;
import tessla2.FlexAnalytics.application.mapper.SensitivityMapper;
import tessla2.FlexAnalytics.domain.model.DataSet;
import tessla2.FlexAnalytics.domain.model.ScenarioSummary;
import tessla2.FlexAnalytics.domain.model.SensitivityResult;
import tessla2.FlexAnalytics.domain.model.CorrelationMethod;
import tessla2.FlexAnalytics.domain.service.CsvExportService;
import tessla2.FlexAnalytics.domain.service.CsvReaderService;
import tessla2.FlexAnalytics.domain.service.ExperimenterDataService;
import tessla2.FlexAnalytics.domain.service.ExperimenterMergeService;
import tessla2.FlexAnalytics.domain.service.SensitivityService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tessla2.FlexAnalytics.application.dto.AnalysisResponse;

@RestController
@RequestMapping("/api/v1/sensitivity")
public class SensitivityController {

    private final CsvReaderService csvReaderService;
    private final SensitivityService sensitivityService;
    private final CsvExportService csvExportService;
    private final SensitivityMapper sensitivityMapper;
    private final ExperimenterMergeService experimenterMergeService;
    private final ExperimenterDataService experimenterDataService;
    private final ScenarioSummaryMapper scenarioSummaryMapper;

    @Value("${app.input-file:classpath:analytics.csv}")
    private String defaultInput;

    @Value("${app.output-file:./output/result.csv}")
    private String defaultOutput;

    @Value("${app.correlation-method:PEARSON}")
    private String defaultCorrelationMethod;

    @Value("${app.merged-output-file:./output/merged_scenarios.csv}")
    private String defaultMergedOutput;

    @Value("${app.upload.max-file-size-bytes:10485760}")
    private long maxUploadFileSizeBytes;

    public SensitivityController(CsvReaderService csvReaderService,
                                 SensitivityService sensitivityService,
                                 CsvExportService csvExportService,
                                 SensitivityMapper sensitivityMapper,
                                 ExperimenterMergeService experimenterMergeService,
                                 ExperimenterDataService experimenterDataService,
                                 ScenarioSummaryMapper scenarioSummaryMapper) {
        this.csvReaderService = csvReaderService;
        this.sensitivityService = sensitivityService;
        this.csvExportService = csvExportService;
        this.sensitivityMapper = sensitivityMapper;
        this.experimenterMergeService = experimenterMergeService;
        this.experimenterDataService = experimenterDataService;
        this.scenarioSummaryMapper = scenarioSummaryMapper;
    }

    @PostMapping("/analyze")
    public AnalysisResponse analyze(
            @RequestParam(required = false) String inputFile,
            @RequestParam(required = false) String outputFile,
            @RequestParam(required = false) String correlationMethod
    ) throws IOException, CsvException {

        String resolvedInput = inputFile == null || inputFile.isBlank() ? defaultInput : inputFile;
        String resolvedOutput = outputFile == null || outputFile.isBlank() ? defaultOutput : outputFile;
        CorrelationMethod resolvedMethod = CorrelationMethod.from(correlationMethod, CorrelationMethod.from(defaultCorrelationMethod, CorrelationMethod.PEARSON));

        DataSet dataSet = csvReaderService.load(resolvedInput);
        List<SensitivityResult> results = sensitivityService.analyze(dataSet, resolvedMethod);
        csvExportService.export(resolvedInput, resolvedOutput, results);

        List<ResultDTO> resultDTOS = results.stream().map(sensitivityMapper::toDto).toList();
        return new AnalysisResponse(resolvedInput, resolvedOutput, dataSet.getRowCount(), dataSet.getNumVars(),
                resolvedMethod.name(), resultDTOS);
    }

    @PostMapping("/merge-experimenter")
    public MergeAnalysisResponse mergeExperimenter(
            @RequestParam List<String> inputFiles,
            @RequestParam(required = false) String outputFile,
            @RequestParam(defaultValue = "ScenarioID") String scenarioColumn,
            @RequestParam(defaultValue = "Object") String objectColumn,
            @RequestParam(defaultValue = "Throughput") String valueColumn
    ) throws IOException, CsvException {

        String resolvedOutput = outputFile == null || outputFile.isBlank() ? defaultMergedOutput : outputFile;
        List<ScenarioSummary> summaries = experimenterMergeService.mergeAndSummarize(
                inputFiles, resolvedOutput, scenarioColumn, objectColumn, valueColumn);
        List<ScenarioSummaryDTO> scenarioDTOs = summaries.stream().map(scenarioSummaryMapper::toDto).toList();

        return new MergeAnalysisResponse(inputFiles, resolvedOutput, scenarioColumn, objectColumn, valueColumn,
                scenarioDTOs.size(), scenarioDTOs);
    }

    @PostMapping("/merge-experimenter/upload")
    public MergeAnalysisResponse mergeExperimenterUpload(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(required = false) String outputFile,
            @RequestParam(defaultValue = "ScenarioID") String scenarioColumn,
            @RequestParam(defaultValue = "Object") String objectColumn,
            @RequestParam(defaultValue = "Throughput") String valueColumn
    ) throws IOException, CsvException {

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one uploaded file is required");
        }

        List<Path> tempFiles = new ArrayList<>();
        try {
            List<String> tempPaths = new ArrayList<>();
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    continue;
                }
                if (file.getSize() > maxUploadFileSizeBytes) {
                    throw new IllegalArgumentException("File '" + file.getOriginalFilename() +
                            "' exceeds max size of " + maxUploadFileSizeBytes + " bytes");
                }
                Path tempFile = Files.createTempFile("experimenter-", ".csv");
                file.transferTo(tempFile);
                tempFiles.add(tempFile);
                tempPaths.add(tempFile.toString());
            }

            if (tempPaths.isEmpty()) {
                throw new IllegalArgumentException("Uploaded files are empty");
            }

            String resolvedOutput = outputFile == null || outputFile.isBlank() ? defaultMergedOutput : outputFile;
            List<ScenarioSummary> summaries = experimenterMergeService.mergeAndSummarize(
                    tempPaths, resolvedOutput, scenarioColumn, objectColumn, valueColumn);
            List<ScenarioSummaryDTO> scenarioDTOs = summaries.stream().map(scenarioSummaryMapper::toDto).toList();

            return new MergeAnalysisResponse(tempPaths, resolvedOutput, scenarioColumn, objectColumn, valueColumn,
                    scenarioDTOs.size(), scenarioDTOs);
        } finally {
            for (Path tempFile : tempFiles) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // best effort cleanup
                }
            }
        }
    }

    @PostMapping("/load-raw")
    public List<String[]> loadRaw(
            @RequestParam String filePath
    ) throws IOException, CsvException {
        return csvReaderService.loadRaw(filePath);
    }

    @PostMapping("/headers")
    public String[] getHeaders(
            @RequestParam String filePath
    ) throws IOException, CsvException {
        return csvReaderService.getHeaders(filePath);
    }

    @PostMapping("/merge-flexsim")
    public Map<String, Map<String, Double>> mergeFlexsim(
            @RequestParam List<String> filePaths,
            @RequestParam List<String> keyColumns,
            @RequestParam(required = false) String separator,
            @RequestParam(required = false) Boolean convertDecimals
    ) throws IOException, CsvException {
        if (keyColumns == null || keyColumns.isEmpty()) {
            keyColumns = List.of("ScenarioID");
        }

        Map<String, List<Map<String, Object>>> raw = experimenterDataService.loadAndMerge(
                filePaths, keyColumns, "value");
        return experimenterDataService.aggregateByKeys(raw);
    }

    @PostMapping("/complete-analysis")
    public CompleteAnalysisResponse completeAnalysis(
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "classpathFiles", required = false) List<String> classpathFiles,
            @RequestParam String outputColumn,
            @RequestParam(required = false) String correlationMethod,
            @RequestParam(required = false) String outputFile
    ) throws IOException, CsvException {

        // Collect all file paths
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
        String resolvedOutput = outputFile == null || outputFile.isBlank() ? defaultOutput : outputFile;

        try {
            // 1. Load and aggregate data
            Map<String, Map<String, Double>> aggregated = experimenterDataService.loadAndMergeAllNumeric(
                    allPaths, List.of("ScenarioID"));

            if (aggregated.isEmpty()) {
                throw new IllegalArgumentException("No data found in files");
            }

            // 2. Auto-detect input columns (exclude output column and metadata)
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

            // 3. Sensitivity analysis
            DataSet dataSet = experimenterDataService.prepareForSensitivity(
                    aggregated, inputColumns, List.of(outputColumn));

            List<SensitivityResult> sensitivityResults = sensitivityService.analyze(dataSet, method);

            // 4. Scenario ranking (best to worst by output)
            List<ScenarioRankingDTO> scenarioRanking = new ArrayList<>();
            List<Map.Entry<String, Map<String, Double>>> sortedScenarios = new ArrayList<>(aggregated.entrySet());
            sortedScenarios.sort((a, b) -> {
                Double outA = a.getValue().getOrDefault(outputColumn, 0.0);
                Double outB = b.getValue().getOrDefault(outputColumn, 0.0);
                return Double.compare(outB, outA); // Descending
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

            // 5. Export if needed
            try {
                csvExportService.export(allPaths.get(0), resolvedOutput, sensitivityResults);
            } catch (Exception e) {
                System.err.println("Warning: Could not export: " + e.getMessage());
            }

            // 6. Build response
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

    @PostMapping("/analyze-flexsim/auto")
    public AnalysisResponse analyzeFlexsimAuto(
            @RequestParam List<String> filePaths,
            @RequestParam String outputColumn,
            @RequestParam(required = false) String correlationMethod,
            @RequestParam(required = false) String outputFile
    ) throws IOException, CsvException {

        CorrelationMethod method = CorrelationMethod.from(correlationMethod, CorrelationMethod.PEARSON);
        String resolvedOutput = outputFile == null || outputFile.isBlank() ? defaultOutput : outputFile;

        Map<String, Map<String, Double>> aggregated = experimenterDataService.loadAndMergeAllNumeric(
                filePaths, List.of("ScenarioID"));

        List<String> inputColumns = new ArrayList<>();
        if (!aggregated.isEmpty()) {
            Set<String> columns = aggregated.values().iterator().next().keySet();
            for (String col : columns) {
                if (!col.equalsIgnoreCase(outputColumn) &&
                    !col.equalsIgnoreCase("ScenarioID") &&
                    !col.equalsIgnoreCase("RepNum") &&
                    !col.equalsIgnoreCase("Object")) {
                    inputColumns.add(col);
                }
            }
        }

        if (inputColumns.isEmpty()) {
            throw new IllegalArgumentException("No input columns found. Check if outputColumn is valid.");
        }

        DataSet dataSet = experimenterDataService.prepareForSensitivity(
                aggregated,
                inputColumns,
                List.of(outputColumn)
        );

        List<SensitivityResult> results = sensitivityService.analyze(dataSet, method);
        
        try {
            csvExportService.export(filePaths.get(0), resolvedOutput, results);
        } catch (Exception e) {
            // Log error but don't fail the analysis
            System.err.println("Warning: Could not export to CSV: " + e.getMessage());
        }

        List<ResultDTO> resultDTOS = results.stream().map(sensitivityMapper::toDto).toList();
        return new AnalysisResponse(filePaths.get(0), resolvedOutput, dataSet.getRowCount(), dataSet.getNumVars(),
                method.name(), resultDTOS);
    }

    @PostMapping("/analyze-flexsim")
    public AnalysisResponse analyzeFlexsim(
            @RequestParam List<String> filePaths,
            @RequestParam(defaultValue = "ScenarioID") String scenarioColumn,
            @RequestParam List<String> inputColumns,
            @RequestParam List<String> outputColumns,
            @RequestParam(required = false) String correlationMethod,
            @RequestParam(required = false) String outputFile
    ) throws IOException, CsvException {

        CorrelationMethod method = CorrelationMethod.from(correlationMethod, CorrelationMethod.PEARSON);
        String resolvedOutput = outputFile == null || outputFile.isBlank() ? defaultOutput : outputFile;

        Map<String, Map<String, Double>> aggregated = experimenterDataService.loadAndMergeAllNumeric(
                filePaths, List.of(scenarioColumn));

        DataSet dataSet = experimenterDataService.prepareForSensitivity(
                aggregated,
                inputColumns,
                outputColumns
        );

        List<SensitivityResult> results = sensitivityService.analyze(dataSet, method);
        csvExportService.export("merged", resolvedOutput, results);

        List<ResultDTO> resultDTOS = results.stream().map(sensitivityMapper::toDto).toList();
        return new AnalysisResponse("merged", resolvedOutput, dataSet.getRowCount(), dataSet.getNumVars(),
                method.name(), resultDTOS);
    }

    @PostMapping("/analyze-flexsim/upload")
    public AnalysisResponse analyzeFlexsimUpload(
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "inputColumns", required = false) List<String> inputColumns,
            @RequestParam(value = "outputColumns", required = false) List<String> outputColumns,
            @RequestParam(required = false) String correlationMethod,
            @RequestParam(required = false) String outputFile,
            @RequestParam(value = "classpathFiles", required = false) List<String> classpathFiles
    ) throws IOException, CsvException {

        List<Path> tempFiles = new ArrayList<>();
        List<String> finalPaths = new ArrayList<>();

        // Process uploaded files
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                if (file.getSize() > maxUploadFileSizeBytes) {
                    throw new IllegalArgumentException("File '" + file.getOriginalFilename() +
                            "' exceeds max size of " + maxUploadFileSizeBytes + " bytes");
                }
                Path tempFile = Files.createTempFile("flexsim-analyze-", ".csv");
                file.transferTo(tempFile);
                tempFiles.add(tempFile);
                finalPaths.add(tempFile.toString());
            }
        }

        // Add classpath files
        if (classpathFiles != null && !classpathFiles.isEmpty()) {
            for (String cpFile : classpathFiles) {
                finalPaths.add("classpath:" + cpFile.trim());
            }
        }

        if (finalPaths.isEmpty()) {
            throw new IllegalArgumentException("No files provided (neither uploaded nor classpath)");
        }

        if (inputColumns == null || inputColumns.isEmpty()) {
            throw new IllegalArgumentException("inputColumns is required");
        }
        if (outputColumns == null || outputColumns.isEmpty()) {
            throw new IllegalArgumentException("outputColumns is required");
        }

        CorrelationMethod method = CorrelationMethod.from(correlationMethod, CorrelationMethod.PEARSON);
        String resolvedOutput = outputFile == null || outputFile.isBlank() ? defaultOutput : outputFile;

        Map<String, Map<String, Double>> aggregated;
        if (finalPaths.stream().anyMatch(p -> p.startsWith("classpath:"))) {
            // Use loadAndMergeAllNumeric for mixed or classpath paths
            aggregated = experimenterDataService.loadAndMergeAllNumeric(finalPaths, List.of("ScenarioID"));
        } else {
            aggregated = experimenterDataService.loadAndMergeAllNumeric(finalPaths, List.of("ScenarioID"));
        }

        DataSet dataSet = experimenterDataService.prepareForSensitivity(aggregated, inputColumns, outputColumns);
        List<SensitivityResult> results = sensitivityService.analyze(dataSet, method);
        csvExportService.export("merged", resolvedOutput, results);

        List<ResultDTO> resultDTOS = results.stream().map(sensitivityMapper::toDto).toList();
        return new AnalysisResponse(String.join(",", finalPaths), resolvedOutput, dataSet.getRowCount(), dataSet.getNumVars(),
                method.name(), resultDTOS);

    }

    @PostMapping("/merge-flexsim/upload")
    public Map<String, Map<String, Double>> mergeFlexsimUpload(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(defaultValue = "ScenarioID") String keyColumn,
            @RequestParam(required = false) String separator,
            @RequestParam(required = false) Boolean convertDecimals,
            @RequestParam(defaultValue = "./output/merged_result.csv") String outputFile
    ) throws IOException, CsvException {

        List<Path> tempFiles = new ArrayList<>();
        try {
            List<String> tempPaths = new ArrayList<>();
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    continue;
                }
                if (file.getSize() > maxUploadFileSizeBytes) {
                    throw new IllegalArgumentException("File too large: " + file.getOriginalFilename());
                }
                Path tempFile = Files.createTempFile("flexsim-", ".csv");
                file.transferTo(tempFile);
                tempFiles.add(tempFile);
                tempPaths.add(tempFile.toString());
            }

            if (tempPaths.isEmpty()) {
                throw new IllegalArgumentException("No files uploaded");
            }

            System.out.println("DEBUG: Loading files: " + tempPaths);
            Map<String, Map<String, Double>> result = experimenterDataService.loadAndMergeAllNumeric(
                    tempPaths, List.of(keyColumn));
            System.out.println("DEBUG: Result size: " + result.size());
            System.out.println("DEBUG: Sample keys: " + (result.isEmpty() ? "empty" : result.keySet().stream().limit(3).toList()));
            System.out.println("DEBUG: Sample columns: " + (result.isEmpty() ? "empty" : result.values().stream().findFirst().get().keySet().stream().limit(5).toList()));

            experimenterDataService.exportToCsv(result, outputFile);
            System.out.println("DEBUG: Export completed to " + outputFile);

            return result;

        } finally {
            for (Path tempFile : tempFiles) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {}
            }
        }
    }

    @PostMapping("/headers/upload")
    public String[] headersUpload(
            @RequestParam("file") MultipartFile file
    ) throws IOException, CsvException {

        Path tempFile = Files.createTempFile("flexsim-headers-", ".csv");
        try {
            file.transferTo(tempFile);
            return csvReaderService.getHeaders(tempFile.toString());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
