package tessla2.FlexAnalytics.runner;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tessla2.FlexAnalytics.domain.model.DataSet;
import tessla2.FlexAnalytics.domain.model.SensitivityResult;
import tessla2.FlexAnalytics.domain.service.CsvExportService;
import tessla2.FlexAnalytics.domain.service.CsvReaderService;
import tessla2.FlexAnalytics.domain.service.SensitivityService;

import java.util.List;

@Component
public class ConsoleRunner implements CommandLineRunner {

    private final CsvReaderService csvReaderService;
    private final SensitivityService sensitivityService;
    private final CsvExportService csvExportService;

    @Value("${app.input-file}")
    private String inputFile;

    @Value("${app.output-file}")
    private String outputFile;

    public ConsoleRunner(CsvReaderService csvReaderService,
                         SensitivityService sensitivityService,
                         CsvExportService csvExportService) {
        this.csvReaderService = csvReaderService;
        this.sensitivityService = sensitivityService;
        this.csvExportService = csvExportService;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=================================");
        System.out.println(" Flex Analytics - Iniciando...");
        System.out.println("=================================");

        System.out.println("\nCarregando arquivo: " + inputFile);
        DataSet dataSet = csvReaderService.load(inputFile);
        System.out.println("Linhas carregadas: " + dataSet.getRowCount());
        System.out.println("Variáveis encontradas: " + dataSet.getNumVars());

        System.out.println("\nExecutando análise de sensibilidade...");
        List<SensitivityResult> results = sensitivityService.analyze(dataSet);

        System.out.println("\n--- Resultado ---");
        results.forEach(r -> System.out.printf(
                "%-20s -> correlação: %+.3f%n",
                r.getVariable(), r.getCorrelation()
        ));

        System.out.println("\nExportando resultados para: " + outputFile);
        csvExportService.export(inputFile, outputFile, results);

        System.out.println("\nConcluído com sucesso!");
        System.out.println("=================================");
    }
}
