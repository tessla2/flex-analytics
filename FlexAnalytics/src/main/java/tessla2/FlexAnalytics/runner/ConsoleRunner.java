//package tessla2.FlexAnalytics.runner;
//
//
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//import tessla2.FlexAnalytics.model.DataSet;
//import tessla2.FlexAnalytics.model.SensitivityResult;
//import tessla2.FlexAnalytics.service.CsvExportService;
//import tessla2.FlexAnalytics.service.CsvReaderService;
//import tessla2.FlexAnalytics.service.SensitivityService;
//
//import java.util.List;
//
//@Component
//public class ConsoleRunner implements CommandLineRunner {
//
//
//    @Autowired private CsvReaderService csvReaderService;
//    @Autowired private SensitivityService sensitivityService;
//    @Autowired private CsvExportService csvExportService;
//
//        @Value("${app.input-file}")
//        private String inputFile;
//
//        @Value("${app.output-file}")
//        private String outputFile;
//
//    @Override
//    public void run(String... args) throws Exception {
//        System.out.println("=================================");
//        System.out.println(" Flex Analytics - Iniciando...");
//        System.out.println("=================================");
//
//        System.out.println("\nCarregando arquivo: " + inputFile);
//        DataSet dataSet = csvReaderService.load(inputFile);
//        System.out.println("Linhas carregadas: " + dataSet.getRowCount());
//        System.out.println("Variáveis encontradas: " + dataSet.getNumVars());
//
//        System.out.println("\nExecutando análise de sensibilidade...");
//        List<SensitivityResult> results = sensitivityService.analyze(dataSet);
//
//        System.out.println("\n--- Resultado ---");
//        results.forEach(r -> System.out.printf(
//                "%-20s -> correlação: %+.3f%n",
//                r.getVariable(), r.getCorrelation()
//        ));
//
//        System.out.println("\nExportando resultados para: " + outputFile);
//        csvExportService.export(inputFile, outputFile, results);
//
//        System.out.println("\nConcluído com sucesso!");
//        System.out.println("=================================");
//    }
//}
