package tessla2.FlexAnalytics.controller;


import com.opencsv.exceptions.CsvException;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tessla2.FlexAnalytics.controller.dto.AnalysisResponse;
import tessla2.FlexAnalytics.controller.dto.ResultDTO;
import tessla2.FlexAnalytics.model.DataSet;
import tessla2.FlexAnalytics.model.SensitivityResult;
import tessla2.FlexAnalytics.service.CsvExportService;
import tessla2.FlexAnalytics.service.CsvReaderService;
import tessla2.FlexAnalytics.service.SensitivityService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/sensitivity")
public class SensitivityController {

    @Autowired
    CsvExportService csvExportService;
    @Autowired
    private CsvReaderService csvReaderService;
    @Autowired
    private SensitivityService sensitivityService;
    @Value("${app.output-file}")
    private String outputFile;


    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnalysisResponse> analyse(
            @RequestParam("file") MultipartFile file) throws IOException, CsvException {

        validateUpload(file);

        Path tempFile = null;

        try {
            tempFile = saveTempFile(file);

            DataSet dataSet = csvReaderService.load(tempFile.toString());
            List<SensitivityResult> results = sensitivityService.analyze(dataSet);

            csvExportService.export(tempFile.toString(), outputFile, results);

            AnalysisResponse response = AnalysisResponse.builder()
                    .fileName(file.getOriginalFilename())
                    .totalRows(dataSet.getRowCount())
                    .totalVariables(dataSet.getNumVars())
                    .outputVariable(dataSet.getOutputHeader())
                    .results(results.stream()
                            .map(r -> ResultDTO.builder()
                                    .variable(r.getVariable())
                                    .correlation(r.getCorrelation())
                                    .absoluteImpact(r.getAbsoluteImpact())
                                    .build())
                            .toList())
                    .build();

            return ResponseEntity.ok(response);

        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
                Files.deleteIfExists(tempFile.getParent());
            }
        }
    }
    @GetMapping("/export")
    public ResponseEntity<Resource> download() throws IOException {
        File file = new File(outputFile);

        if (!file.exists())
            return ResponseEntity.notFound().build();

        Resource resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getName() + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }


    // ---- helpers ----

    private void validateUpload(MultipartFile file) {
        if (file.isEmpty())
            throw new IllegalArgumentException("Empty file");

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.endsWith(".csv"))
            throw new IllegalArgumentException("Only .csv files allowed.");

        if (file.getSize() > 10L * 1024 * 1024)
            throw new IllegalArgumentException("File exceeds 10MB limit.");
    }

    private Path saveTempFile(MultipartFile file) throws IOException {
        Path tempDir = Files.createTempDirectory("flex-analytics");
        Path tempFile = tempDir.resolve(
                Objects.requireNonNull(file.getOriginalFilename()));
        file.transferTo(tempFile);
        return tempFile;
    }


}
