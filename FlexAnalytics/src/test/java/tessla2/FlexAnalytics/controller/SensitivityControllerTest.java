package tessla2.FlexAnalytics.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import tessla2.FlexAnalytics.application.dto.ResultDTO;
import tessla2.FlexAnalytics.application.dto.ScenarioSummaryDTO;
import tessla2.FlexAnalytics.application.mapper.ScenarioSummaryMapper;
import tessla2.FlexAnalytics.application.mapper.SensitivityMapper;
import tessla2.FlexAnalytics.common.GlobalExceptionHandler;
import tessla2.FlexAnalytics.domain.model.DataSet;
import tessla2.FlexAnalytics.domain.model.ScenarioSummary;
import tessla2.FlexAnalytics.domain.model.SensitivityResult;
import tessla2.FlexAnalytics.domain.service.CsvExportService;
import tessla2.FlexAnalytics.domain.service.CsvReaderService;
import tessla2.FlexAnalytics.domain.service.ExperimenterMergeService;
import tessla2.FlexAnalytics.domain.service.SensitivityService;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SensitivityController.class)
@Import(GlobalExceptionHandler.class)
class SensitivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CsvReaderService csvReaderService;
    @MockitoBean
    private SensitivityService sensitivityService;
    @MockitoBean
    private CsvExportService csvExportService;
    @MockitoBean
    private SensitivityMapper sensitivityMapper;
    @MockitoBean
    private ExperimenterMergeService experimenterMergeService;
    @MockitoBean
    private ScenarioSummaryMapper scenarioSummaryMapper;

    @Test
    void shouldAnalyzeAndReturnResponse() throws Exception {
        DataSet dataSet = new DataSet(new String[]{"x1", "y"}, List.of(new double[]{1}), new double[]{2}, 1);
        SensitivityResult result = new SensitivityResult("x1", 0.9);

        when(csvReaderService.load(any())).thenReturn(dataSet);
        when(sensitivityService.analyze(any(DataSet.class), any())).thenReturn(List.of(result));
        when(sensitivityMapper.toDto(any(SensitivityResult.class))).thenReturn(new ResultDTO("x1", 0.9, 0.9));

        mockMvc.perform(post("/api/v1/sensitivity/analyze")
                        .param("inputFile", "classpath:analytics.csv")
                        .param("outputFile", "./output/result.csv")
                        .param("correlationMethod", "SPEARMAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correlationMethod").value("SPEARMAN"))
                .andExpect(jsonPath("$.results[0].variable").value("x1"));
    }

    @Test
    void shouldMergeUploadedFiles() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "exp.csv",
                "text/csv",
                "ScenarioID,RepNum,Object,Throughput\n1,1,A,10\n".getBytes()
        );

        when(experimenterMergeService.mergeAndSummarize(anyList(), any(), any(), any(), any()))
                .thenReturn(List.of(new ScenarioSummary(1, 10.0, Map.of("A", 10.0))));
        when(scenarioSummaryMapper.toDto(any(ScenarioSummary.class)))
                .thenReturn(new ScenarioSummaryDTO(1, 10.0, Map.of("A", 10.0)));

        mockMvc.perform(multipart("/api/v1/sensitivity/merge-experimenter/upload")
                        .file(file)
                        .param("scenarioColumn", "ScenarioID")
                        .param("objectColumn", "Object")
                        .param("valueColumn", "Throughput"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenarioCount").value(1));
    }

    @Test
    void shouldReturnBadRequestWhenUploadedFilesAreEmpty() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile("files", "empty.csv", "text/csv", new byte[0]);

        mockMvc.perform(multipart("/api/v1/sensitivity/merge-experimenter/upload").file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").value("Invalid input data"));
    }
}
