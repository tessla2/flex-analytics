package tessla2.FlexAnalytics.config;

import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Flex Analytics API")
                        .description("""
                                API for sensitivity analysis of factory floor data.
                                
                                                    Allows CSV file upload, Pearson correlation processing
                                                    between input and output variables, and result exportation.
                                
                                                    **How it works:**
                                                    1. Upload a CSV file via `POST /api/v1/sensitivity/analyze`
                                                    2. The API calculates Pearson correlation between each input variable and the output
                                                    3. Results are returned ordered by absolute impact descending
                                                    4. The exported CSV with results can be downloaded via `GET /api/v1/sensitivity/export`
                                
                                                    **Expected CSV format:**
                                                    - First row must contain headers
                                                    - Last column is treated as the output variable
                                                    - All other columns are treated as input variables
                                                    - All values must be numeric
                                                    - Maximum file size: 10MB
                """)
                                .version("v1.0.0")
//                                .contact(new Contact()
//                                        .name("Flex Analytics")
//                                        .email(""))
                                .license(new License()
                                        .name("Apache 2.0")
                                        .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addServersItem(new Server()
                        .url("http://localhost:8080")
                        .description("Local"))
//                .addServersItem(new Server()
//                        .url("https://api.flexanalytics.com")
//                        .description("Produção"))
                .components(new Components()
                        .addSecuritySchemes("basicAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")
                                        .description("Access Credentials to API")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("basicAuth"));
    }
}
