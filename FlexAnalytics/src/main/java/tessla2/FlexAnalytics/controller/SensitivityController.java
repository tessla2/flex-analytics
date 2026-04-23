package tessla2.FlexAnalytics.controller;

import com.opencsv.exceptions.CsvException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tessla2.FlexAnalytics.application.dto.AnalysisResponse;
import tessla2.FlexAnalytics.application.dto.MergeAnalysisResponse;
import tessla2.FlexAnalytics.application.dto.ResultDTO;
import tessla2.FlexAnalytics.application.dto.ScenarioSummaryDTO;
import tessla2.FlexAnalytics.application.mapper.ScenarioSummaryMapper;
import tessla2.FlexAnalytics.application.mapper.SensitivityMapper;
import tessla2.FlexAnalytics.domain.model.DataSet;
import tessla2.FlexAnalytics.domain.model.ScenarioSummary;
import tessla2.FlexAnalytics.domain.model.SensitivityResult;
import tessla2.FlexAnalytics.domain.model.CorrelationMethod;
import tessla2.FlexAnalytics.domain.service.CsvExportService;
import tessla2.FlexAnalytics.domain.service.CsvReaderService;
import tessla2.FlexAnalytics.domain.service.ExperimenterMergeService;
import tessla2.FlexAnalytics.domain.service.SensitivityService;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sensitivity")
public class SensitivityController {

    private final CsvReaderService csvReaderService;
    private final SensitivityService sensitivityService;
    private final CsvExportService csvExportService;
    private final SensitivityMapper sensitivityMapper;
    private final ExperimenterMergeService experimenterMergeService;
    private final ScenarioSummaryMapper scenarioSummaryMapper;

    @Value("${app.input-file:classpath:analytics.csv}")
    private String defaultInput;

    @Value("${app.output-file:./output/result.csv}")
    private String defaultOutput;

    @Value("${app.correlation-method:PEARSON}")
    private String defaultCorrelationMethod;

    @Value("${app.merged-output-file:./output/merged_scenarios.csv}")
    private String defaultMergedOutput;

    public SensitivityController(CsvReaderService csvReaderService,
                                 SensitivityService sensitivityService,
                                 CsvExportService csvExportService,
                                 SensitivityMapper sensitivityMapper,
                                 ExperimenterMergeService experimenterMergeService,
                                 ScenarioSummaryMapper scenarioSummaryMapper) {
        this.csvReaderService = csvReaderService;
        this.sensitivityService = sensitivityService;
        this.csvExportService = csvExportService;
        this.sensitivityMapper = sensitivityMapper;
        this.experimenterMergeService = experimenterMergeService;
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
}
