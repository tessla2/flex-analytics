package tessla2.FlexAnalytics.controller;


import com.opencsv.exceptions.CsvException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import tessla2.FlexAnalytics.controller.dto.ErrorResponse;
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
@Tag(
        name = "Sensitivity Analysis",
        description = "Endpoints for upload, processing and export of factory floor sensitivity analysis"
)
public class SensitivityController {

    @Autowired CsvExportService csvExportService;
    @Autowired CsvReaderService csvReaderService;
    @Autowired SensitivityService sensitivityService;

    @Value("${app.output-file}")
    private String outputFile;

    @Operation(
            summary = "Analyze CSV file",
            description = """
            Receives a CSV file with factory floor data and returns sensitivity analysis.
            
            **File requirements:**
            - Extension must be `.csv`
            - Maximum size: 10MB
            - First row: headers
            - Last column: output variable
            - All other columns: input variables
            - All values must be numeric
            
            **Result:**
            - ANOVA/Pearson correlation of each input variable with the output
            - Ordered by absolute impact descending
            - Values between -1 and +1
        """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Analysis completed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AnalysisResponse.class),
                            examples = @ExampleObject(
                                    name = "Success response",
                                    value = """
                    {
                        "fileName": "analytics.csv",
                        "totalRows": 10,
                        "totalVariables": 3,
                        "outputVariable": "throughput",
                        "results": [
                            {
                                "variable": "operators",
                                "correlation": 0.996,
                                "absoluteImpact": 0.996
                            },
                            {
                                "variable": "machine_time",
                                "correlation": -0.996,
                                "absoluteImpact": 0.996
                            },
                            {
                                "variable": "buffer",
                                "correlation": 0.960,
                                "absoluteImpact": 0.960
                            }
                        ]
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or malformed file",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Empty file",
                                            value = """
                        {
                            "status": 400,
                            "message": "Empty file.",
                            "timestamp": "2026-03-03T17:00:00"
                        }
                        """
                                    ),
                                    @ExampleObject(
                                            name = "Invalid extension",
                                            value = """
                        {
                            "status": 400,
                            "message": "Only .csv files are allowed.",
                            "timestamp": "2026-03-03T17:00:00"
                        }
                        """
                                    ),
                                    @ExampleObject(
                                            name = "File too large",
                                            value = """
                        {
                            "status": 400,
                            "message": "File exceeds the 10MB limit.",
                            "timestamp": "2026-03-03T17:00:00"
                        }
                        """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                    {
                        "status": 500,
                        "message": "An unexpected error occurred.",
                        "timestamp": "2026-03-03T17:00:00"
                    }
                    """
                            )
                    )
            )
    })
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnalysisResponse> analyse(
            @Parameter(
                    description = "CSV file containing factory floor data",
                    required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
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
                                    .analysisType(r.getAnalysisType())
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

    @Operation(
            summary = "Download exported CSV",
            description = """
            Downloads the original CSV with analysis columns appended.
            
            **Appended columns:**
            - `top_variable`: name of the variable with the highest absolute correlation
            - `correlation`: Pearson or ANOVA correlation value of the top variable
            
            **Prerequisite:** `/analyze` endpoint must have been called first.
        """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "CSV file available for download",
                    content = @Content(mediaType = "text/csv")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(hidden = true))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No exported analysis found yet",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    @GetMapping("/export")
    public ResponseEntity<?> download() throws IOException { //? é um wildcard do Java that means "unknown type in compilation time"
        File file = new File(outputFile);

        if (!file.exists())
            return ResponseEntity.status(404)
                    .body(new ErrorResponse(404, "Analysis not found / call analyse first"));

        Resource resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getName() + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }

    private void validateUpload(MultipartFile file) {
        if (file.isEmpty())
            throw new IllegalArgumentException("Empty file.");

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.endsWith(".csv"))
            throw new IllegalArgumentException("Only .csv files are allowed.");

        if (file.getSize() > 10L * 1024 * 1024)
            throw new IllegalArgumentException("File exceeds the 10MB limit.");
    }

    private Path saveTempFile(MultipartFile file) throws IOException {
        Path tempDir = Files.createTempDirectory("flex-analytics");
        Path tempFile = tempDir.resolve(
                Objects.requireNonNull(file.getOriginalFilename()));
        file.transferTo(tempFile);
        return tempFile;
    }
}