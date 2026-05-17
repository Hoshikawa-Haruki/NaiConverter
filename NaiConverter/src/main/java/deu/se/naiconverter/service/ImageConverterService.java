package deu.se.naiconverter.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

@Slf4j
@Service
public class ImageConverterService {

    public Path convertToJpg(Path pngPath, Path outputDir, float quality) throws IOException {
        BufferedImage original = ImageIO.read(pngPath.toFile());
        if (original == null) {
            throw new IOException("이미지를 읽을 수 없음: " + pngPath);
        }

        // PNG의 투명도(알파채널)를 흰 배경으로 처리
        BufferedImage jpgImage = new BufferedImage(
            original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB
        );
        Graphics2D g = jpgImage.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, jpgImage.getWidth(), jpgImage.getHeight());
        g.drawImage(original, 0, 0, null);
        g.dispose();

        String jpgFilename = pngPath.getFileName().toString()
            .replaceAll("(?i)\\.png$", ".jpg");
        Path outputPath = outputDir.resolve(jpgFilename);

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam params = writer.getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(quality);

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputPath.toFile())) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(jpgImage, null, null), params);
        } finally {
            writer.dispose();
        }

        long pngSize = pngPath.toFile().length();
        long jpgSize = outputPath.toFile().length();
        String saved = String.format("%.1f", (1.0 - (double) jpgSize / pngSize) * 100);
        log.info("[변환 완료] {} -> {} ({}KB -> {}KB, {}% 절약)",
            pngPath.getFileName(), outputPath.getFileName(),
            pngSize / 1024, jpgSize / 1024, saved
        );
        return outputPath;
    }
}
