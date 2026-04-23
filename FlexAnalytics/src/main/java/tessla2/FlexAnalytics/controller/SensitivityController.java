package tessla2.FlexAnalytics.controller;

import com.opencsv.exceptions.CsvException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tessla2.FlexAnalytics.application.dto.AnalysisResponse;
import tessla2.FlexAnalytics.application.dto.ResultDTO;
import tessla2.FlexAnalytics.application.mapper.SensitivityMapper;
import tessla2.FlexAnalytics.domain.model.DataSet;
import tessla2.FlexAnalytics.domain.model.SensitivityResult;
import tessla2.FlexAnalytics.domain.service.CsvExportService;
import tessla2.FlexAnalytics.domain.service.CsvReaderService;
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

    @Value("${app.input-file:classpath:analytics.csv}")
    private String defaultInput;

    @Value("${app.output-file:./output/result.csv}")
    private String defaultOutput;

    public SensitivityController(CsvReaderService csvReaderService,
                                 SensitivityService sensitivityService,
                                 CsvExportService csvExportService,
                                 SensitivityMapper sensitivityMapper) {
        this.csvReaderService = csvReaderService;
        this.sensitivityService = sensitivityService;
        this.csvExportService = csvExportService;
        this.sensitivityMapper = sensitivityMapper;
    }

    @PostMapping("/analyze")
    public AnalysisResponse analyze(
            @RequestParam(required = false) String inputFile,
            @RequestParam(required = false) String outputFile
    ) throws IOException, CsvException {

        String resolvedInput = inputFile == null || inputFile.isBlank() ? defaultInput : inputFile;
        String resolvedOutput = outputFile == null || outputFile.isBlank() ? defaultOutput : outputFile;

        DataSet dataSet = csvReaderService.load(resolvedInput);
        List<SensitivityResult> results = sensitivityService.analyze(dataSet);
        csvExportService.export(resolvedInput, resolvedOutput, results);

        List<ResultDTO> resultDTOS = results.stream().map(sensitivityMapper::toDto).toList();
        return new AnalysisResponse(resolvedInput, resolvedOutput, dataSet.getRowCount(), dataSet.getNumVars(), resultDTOS);
    }
}
