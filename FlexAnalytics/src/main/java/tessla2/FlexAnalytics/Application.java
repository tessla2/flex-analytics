package tessla2.FlexAnalytics;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableConfigurationProperties
public class Application {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure()
						.ignoreIfMissing()
								.load();
				dotenv.entries().forEach(e ->
				System.setProperty(e.getKey(), e.getValue())
				);
		SpringApplication.run(Application.class, args);
	}
}
