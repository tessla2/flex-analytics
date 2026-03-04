package tessla2.FlexAnalytics.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;

import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tessla2.FlexAnalytics.model.DataSet;
import tessla2.FlexAnalytics.model.SensitivityResult;
import tessla2.FlexAnalytics.service.CsvExportService;
import tessla2.FlexAnalytics.service.CsvReaderService;
import tessla2.FlexAnalytics.service.SensitivityService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SensitivityController.class)
@WithMockUser(username = "admin", password = "admin_password")
public class SensitivityControlerTest {

    @Autowired
    private MockMvc mockMvc;


    @MockitoBean private CsvReaderService csvReaderService;
    @MockitoBean private SensitivityService sensitivityService;
    @MockitoBean private CsvExportService csvExportService;

    @Test
    void shouldReturn200WhenSendingValidCSV() throws Exception {

        DataSet mockDataSet = new DataSet(
                new String[]{"temperature", "production"}, 1,
                List.of(new double[]{72.5}), new double[]{520}
        );
        List<SensitivityResult> mockResults =List.of(
                new SensitivityResult("temperature", 0.987)
        );

        when(csvReaderService.load(any())).thenReturn(mockDataSet);
        when(sensitivityService.analyze(any())).thenReturn(mockResults);

        MockMultipartFile file = new MockMultipartFile(
                "file", "analytics.csv",
                "text/csv", "temperature,production\n72.5,520".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/sensitivity/analyze").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVariables").value(1))
                .andExpect(jsonPath("$.results[0].variable").value("temperature"))
                .andExpect(jsonPath("$.results[0].correlation").value(0.987));
    }
    @Test
    void shouldReturn400ForEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.csv", "text/csv", new byte[0]
        );

        mockMvc.perform(multipart("/api/v1/sensitivity/analyze").file(file))
                .andExpect(status().isBadRequest());
    }




    }
