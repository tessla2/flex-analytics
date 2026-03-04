package tessla2.FlexAnalytics.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity  // enables Spring Security's web security support
public class SecurityConfig {


    /// Basic authentication is used for simplicity. In production, consider more secure options and proper credential management.
    /// ****Change method in production to use a more secure authentication mechanism and properly manage credentials.****
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                 // Disable for API REST
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session -> session
                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        );
        return http.build();
    }
}
