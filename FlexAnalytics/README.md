# Flex Analytics API

Spring Boot API for sensitivity analysis of factory floor data using ANOVA/Pearson correlation.

## Requirements
- Java 21+
- Maven 3.8+

## Setup

Create a `.env` file at the project root:

ADMIN_USER=admin
ADMIN_PASSWORD=yourpassword
INPUT_FILE=analytics.csv
OUTPUT_FILE=output/analytics_output.csv
LOG_FILE=logs/flex-analytics.log

## Running

mvn spring-boot:run

## Documentation

Swagger UI: http://localhost:8080/swagger-ui.html
API Docs:   http://localhost:8080/api-docs

## CSV Format

| Column       | Type   | Description         |
|--------------|--------|---------------------|
| variable_1..N | double | Input variables     |
| last_column  | double | Output variable     |

Example:

operators,machine_time,buffer,throughput
3,12.5,100,520
4,10.2,150,610
2,15.1,80,470

## Endpoints

| Method | Endpoint                        | Description                  |
|--------|---------------------------------|------------------------------|
| POST   | /api/v1/sensitivity/analyze     | Upload and analyze CSV       |
| GET    | /api/v1/sensitivity/export      | Download exported CSV        |
| GET    | /actuator/health                | Application health check     |

## Authentication

Basic Auth required on all endpoints except `/actuator/health`.

## Error Codes

| Status | Description                        |
|--------|------------------------------------|
| 400    | Invalid or malformed request       |
| 401    | Missing or invalid credentials     |
| 404    | Resource not found                 |
| 500    | Internal server error              |