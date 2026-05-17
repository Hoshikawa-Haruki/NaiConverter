package deu.se.naiconverter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "converter")
public class ConverterProperties {
    private String watchFolder;
    private String outputFolder;
    private float jpgQuality = 0.85f;
    private boolean deleteOriginal = false;
    private boolean processExisting = false;
}
