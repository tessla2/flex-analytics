package tessla2.FlexAnalytics.application.dto;

public class ScenarioRankingDTO {
    private String scenarioId;
    private double outputValue;
    private int rank;
    
    public ScenarioRankingDTO() {}
    
    public ScenarioRankingDTO(String scenarioId, double outputValue, int rank) {
        this.scenarioId = scenarioId;
        this.outputValue = outputValue;
        this.rank = rank;
    }
    
    public String getScenarioId() { return scenarioId; }
    public void setScenarioId(String scenarioId) { this.scenarioId = scenarioId; }
    
    public double getOutputValue() { return outputValue; }
    public void setOutputValue(double outputValue) { this.outputValue = outputValue; }
    
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
}
