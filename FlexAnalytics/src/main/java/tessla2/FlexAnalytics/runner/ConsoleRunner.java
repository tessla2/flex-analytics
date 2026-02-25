package tessla2.FlexAnalytics.runner;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tessla2.FlexAnalytics.model.DataSet;
import tessla2.FlexAnalytics.model.SensitivityResult;
import tessla2.FlexAnalytics.service.CsvReaderService;
import tessla2.FlexAnalytics.service.SensitivityService;

import java.util.List;

@Component
public class ConsoleRunner implements CommandLineRunner {


    @Autowired private CsvReaderService csvReaderService;
    @Autowired private SensitivityService sensitivityService;
  //  @Autowired private CsvExportService csvExportService;

        @Value("${app.input-file}")
        private String inputFile;

        @Value("${app.output-file}")
        private String outputFile;

        @Override
        public void run(String... args) throws Exception {
            System.out.println("Carregando dados: " + inputFile);
            DataSet dataSet = csvReaderService.load(inputFile);

            System.out.println("Executando análise de sensibilidade...\n");
            List<SensitivityResult> results = sensitivityService.analyze(dataSet);

            results.forEach(r -> System.out.printf(
                    "%-20s -> impacto: %+.3f%n", r.getVariable(), r.getCorrelation()));

//            csvExportService.export(inputFile, outputFile, results);
//            System.out.println("\nResultados exportados para: " + outputFile);
        }
    }
