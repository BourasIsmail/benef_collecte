package ma.entraide.suivi;

import ma.entraide.suivi.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class SuiviApplication {
    public static void main(String[] args) {
        SpringApplication.run(SuiviApplication.class, args);
    }
}
