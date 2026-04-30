package tessla2.FlexAnalytics.application.dto;

import java.util.List;
import java.util.Map;

public class CompleteAnalysisResponse {
    private String inputFile;
    private String correlationMethod;
    private int scenarioCount;
    private int variableCount;
    
    // Sensitivity analysis - what impacts the output
    private List<ResultDTO> sensitivityResults;
    
    // Scenario ranking - best to worst
    private List<ScenarioRankingDTO> scenarioRanking;
    
    // Best scenario details
    private String bestScenarioId;
    private Map<String, Double> bestScenarioConfig;
    
    public CompleteAnalysisResponse() {}
    
    public CompleteAnalysisResponse(String inputFile, String correlationMethod, 
                                  int scenarioCount, int variableCount,
                                  List<ResultDTO> sensitivityResults,
                                  List<ScenarioRankingDTO> scenarioRanking,
                                  String bestScenarioId, 
                                  Map<String, Double> bestScenarioConfig) {
        this.inputFile = inputFile;
        this.correlationMethod = correlationMethod;
        this.scenarioCount = scenarioCount;
        this.variableCount = variableCount;
        this.sensitivityResults = sensitivityResults;
        this.scenarioRanking = scenarioRanking;
        this.bestScenarioId = bestScenarioId;
        this.bestScenarioConfig = bestScenarioConfig;
    }
    
    public String getInputFile() { return inputFile; }
    public void setInputFile(String inputFile) { this.inputFile = inputFile; }
    
    public String getCorrelationMethod() { return correlationMethod; }
    public void setCorrelationMethod(String correlationMethod) { this.correlationMethod = correlationMethod; }
    
    public int getScenarioCount() { return scenarioCount; }
    public void setScenarioCount(int scenarioCount) { this.scenarioCount = scenarioCount; }
    
    public int getVariableCount() { return variableCount; }
    public void setVariableCount(int variableCount) { this.variableCount = variableCount; }
    
    public List<ResultDTO> getSensitivityResults() { return sensitivityResults; }
    public void setSensitivityResults(List<ResultDTO> sensitivityResults) { this.sensitivityResults = sensitivityResults; }
    
    public List<ScenarioRankingDTO> getScenarioRanking() { return scenarioRanking; }
    public void setScenarioRanking(List<ScenarioRankingDTO> scenarioRanking) { this.scenarioRanking = scenarioRanking; }
    
    public String getBestScenarioId() { return bestScenarioId; }
    public void setBestScenarioId(String bestScenarioId) { this.bestScenarioId = bestScenarioId; }
    
    public Map<String, Double> getBestScenarioConfig() { return bestScenarioConfig; }
    public void setBestScenarioConfig(Map<String, Double> bestScenarioConfig) { this.bestScenarioConfig = bestScenarioConfig; }
}
