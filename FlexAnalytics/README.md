# Flex Analytics API

Spring Boot REST API for sensitivity analysis of FlexSim factory floor simulation data, supporting Pearson correlation and ANOVA.

## Requirements
- Java 21+
- Maven 3.8+

## Quick Start

```bash
cd FlexAnalytics
./mvnw.cmd spring-boot:run
```
## Configuration (Optional)

Create a `.env` file at the project root to customize:

```
ADMIN_USER=admin
ADMIN_PASSWORD=yourpassword
INPUT_FILE=analytics.csv
OUTPUT_FILE=output/analytics_output.csv
LOG_FILE=logs/app.log
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/sensitivity/analyze` | Upload CSV and run analysis |
| `GET` | `/api/v1/sensitivity/export` | Download enriched CSV with results |
| `GET` | `/actuator/health` | Health check |

## Testing with Postman

1. **Method:** `POST`
2. **URL:** `http://localhost:8081/api/v1/sensitivity/analyze`
3. **Auth:** Basic Auth → `admin` / `123456`
4. **Body:** `form-data`
   - Key: `file` (type: File) → select your `.csv` file
5. **Send!**

## Supported CSV Types

The API automatically detects the CSV type and applies the appropriate analysis.

### 1. Aggregated Production Data
Standard numeric columns. Last column is the output variable.

```
operadores,tempo_maquina,buffer,throughput
3,12.5,100,520
4,10.2,150,610
```

### 2. FlexSim Experimenter Export (Multi-Scenario)
Exported from the FlexSim Experimenter module. `ScenarioID` and `RepNum` are automatically detected as metadata and excluded from analysis.

```
ScenarioID;RepNum;factor1;factor2;response
1;1;10;100;520
1;2;10;100;515
2;1;15;150;610
```

### 3. Time Series
Date column followed by a metric. Automatically aggregated by hour of day.

```
Time;Quantidade
12/06/2025 00:00:05;1,000000
12/06/2025 01:38:47;1,000000
```

### 4. Object State (FlexSim)
State column with text values (e.g. `Travel loaded`, `Idle`). Automatically label-encoded and analyzed with ANOVA.

```
ScenarioID;RepNum;Object;State;Time;Utilization
1;1;AGILOX;Travel loaded;825,56;0,987500
1;1;AGILOX;Idle;0,000000;0,987500
```

### 5. Sensor / Battery Data
Numeric metric recorded over time with a date column.

```
ScenarioID;RepNum;AMR;Nivel da bateria;Tempo
1;1;0;99,93;26/01/2026 00:00:24
```

---

## Auto-Detection Rules

| Rule | Behavior |
|------|----------|
| `ScenarioID`, `RepNum` columns | Always ignored (FlexSim metadata) |
| Constant columns (zero/near-zero variance) | Ignored automatically |
| Text columns with variation | Label-encoded → analyzed with ANOVA |
| Text columns that are constant | Ignored |
| Date/time columns | Converted to epoch seconds (UTC) |
| Separator | Auto-detected: `;` `,` `\t` `\|` |
| Decimal | Auto-normalized: `,` → `.` |
| Output variable | Last active column with variance |
| Time series | Date as first active column → aggregated by hour |

---

## Analysis Methods

The API automatically selects the analysis method per variable:

| Variable Type | Method | Effect Size |
|-------------|--------|------------|
| Numeric | Pearson correlation | r ∈ [-1, 1] |
| Categorical (text) | ANOVA (one-way) | eta-squared ∈ [0, 1] |

---

## Request — POST /api/v1/sensitivity/analyze

**Content-Type:** `multipart/form-data`
**Parameter:** `file` — `.csv` file

### Response

```json
{
  "fileName": "Estado_do_Agilox.csv",
  "totalRows": 1200,
  "totalVariables": 2,
  "outputVariable": "Time",
  "results": [
    {
      "variable": "State",
      "correlation": 0.9919,
      "absoluteImpact": 0.9919,
      "analysisType": "ANOVA"
    },
    {
      "variable": "ScenarioID",
      "correlation": -0.1155,
      "absoluteImpact": 0.1155,
      "analysisType": "Pearson"
    }
  ]
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `fileName` | string | Original file name |
| `totalRows` | int | Number of data rows after processing |
| `totalVariables` | int | Number of input variables analyzed |
| `outputVariable` | string | Name of the output (target) variable |
| `results[].variable` | string | Input variable name |
| `results[].correlation` | double | Pearson r or ANOVA eta-squared |
| `results[].absoluteImpact` | double | Absolute value of correlation / eta-squared |
| `results[].analysisType` | string | `"Pearson"` or `"ANOVA"` |

Results are ordered by `absoluteImpact` descending.

---

## Authentication

Basic Auth is required on all endpoints except `/actuator/health`.

```
Authorization: Basic <base64(admin:123456)>
```

---

## Error Codes

| Status | Description |
|--------|-------------|
| 400 | Invalid or malformed request (empty file, wrong format, parse error) |
| 401 | Missing or invalid credentials |
| 404 | Resource not found (e.g. export called before analyze) |
| 500 | Internal server error |

### Error Response Body

```json
{
  "status": 400,
  "message": "Only .csv files are allowed.",
  "timestamp": "2026-03-05T12:00:00"
}
```