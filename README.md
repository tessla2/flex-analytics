# Flex Analytics API

Spring Boot REST API for sensitivity analysis of FlexSim factory floor simulation data, supporting Pearson and Spearman correlation.

## Requirements

- Java 21+
- Maven 3.8+

## Quick Start

```bash
cd FlexAnalytics
./mvnw.cmd spring-boot:run
```

The API will be available at `http://localhost:8081`

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
| `POST` | `/api/v1/sensitivity/merge-flexsim/upload` | Merge FlexSim experimenter files |
| `POST` | `/api/v1/sensitivity/headers/upload` | Get CSV headers |
| `GET` | `/actuator/health` | Health check |

## FlexSim Experimenter Workflow

### 1. Discover CSV Headers

```bash
POST /api/v1/sensitivity/headers/upload
```

**Body:** `form-data`
- Key: `file` (type: File)

**Response:**
```json
["ScenarioID", "RepNum", "Object", "State", "Time", "Utilization"]
```

### 2. Merge FlexSim Files

```bash
POST /api/v1/sensitivity/merge-flexsim/upload
```

**Body:** `form-data`
- Key: `files` (type: File) - select multiple CSV files
- Key: `keyColumn` (type: Text) - default: `ScenarioID`
- Key: `outputFile` (type: Text) - default: `./output/merged_result.csv`

**Response:**
```json
{
  "1": {
    "Utilization": 0.333571,
    "Time": 125.172414
  },
  "2": {
    "Utilization": 0.450000,
    "Time": 150.000000
  }
}
```

The output file is saved to `FlexAnalytics/output/merged_result.csv`

### 3. Run Sensitivity Analysis

After merging, use the summary file with input/output columns for correlation analysis.

## Supported CSV Formats

The API automatically detects:
- **Delimiter:** `;` or `,`
- **Decimal:** `,` (comma) or `.` (dot)

Example FlexSim Experimenter export:
```
ScenarioID;RepNum;Object;State;Time;Utilization
1;000000;Operador A1;Processing;0,000000;0,333571
1;000000;Operador A1;Busy;0,000000;0,333571
```

## CSV Format Detection Rules

| Rule | Behavior |
|------|----------|
| `ScenarioID`, `RepNum` columns | Used as keys for merging |
| Separator | Auto-detected: `;` or `,` |
| Decimal | Auto-normalized: `,` → `.` |
| Numeric columns | Merged by key column |

## Authentication

Basic Auth is required on all endpoints except `/actuator/health`.

```
Authorization: Basic <base64(admin:123456)>
```

## Testing with Postman

1. **Method:** `POST`
2. **URL:** `http://localhost:8081/api/v1/sensitivity/merge-flexsim/upload`
3. **Auth:** Basic Auth → `admin` / `123456`
4. **Body:** `form-data`
   - Key: `files` (type: File) → select your `.csv` files
   - Key: `keyColumn` (type: Text) → `ScenarioID`
5. **Send!**

## Error Codes

| Status | Description |
|--------|-------------|
| 400 | Invalid or malformed request |
| 401 | Missing or invalid credentials |
| 500 | Internal server error |

### Error Response Body

```json
{
  "status": 400,
  "message": "Error description",
  "timestamp": "2026-04-28T12:00:00"
}
```

## Project Structure

```
FlexAnalytics/src/main/java/tessla2/FlexAnalytics/
├── controller/
│   └── SensitivityController.java
├── domain/
│   ├── model/
│   │   ├── DataSet.java
│   │   ├── SensitivityResult.java
│   │   └── CorrelationMethod.java
│   └── service/
│       ├── CsvReaderService.java
│       ├── CsvExportService.java
│       ├── SensitivityService.java
│       ├── ExperimenterDataService.java
│       └── ExperimenterMergeService.java
├── application/
│   ├── dto/
│   └── mapper/
├── exception/
└── runner/
    └── ConsoleRunner.java
```

## Future Enhancements

- ANOVA for categorical variables
- Time series aggregation
- Dashboard with charts
- Export to Excel
- Multiple output columns
