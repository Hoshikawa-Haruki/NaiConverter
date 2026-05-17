package deu.se.naiconverter.service;

import deu.se.naiconverter.config.ConverterProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FolderWatcherService {

    private final ConverterProperties props;
    private final ImageConverterService converterService;

    @PostConstruct
    public void start() {
        if (props.getWatchFolder() == null || props.getWatchFolder().isBlank()) {
            log.error("converter.watch-folder가 설정되지 않았습니다. application.properties를 확인해주세요.");
            return;
        }

        Path watchPath = Path.of(props.getWatchFolder());
        Path outputPath = (props.getOutputFolder() != null && !props.getOutputFolder().isBlank())
            ? Path.of(props.getOutputFolder())
            : watchPath;

        if (!Files.isDirectory(watchPath)) {
            log.error("감시 폴더가 존재하지 않습니다: {}", watchPath);
            return;
        }

        try {
            Files.createDirectories(outputPath);
        } catch (IOException e) {
            log.error("출력 폴더 생성 실패: {}", outputPath, e);
            return;
        }

        log.info("=== NaiConverter 시작 ===");
        log.info("감시 폴더: {}", watchPath.toAbsolutePath());
        log.info("저장 폴더: {}", outputPath.toAbsolutePath());
        log.info("JPG 품질:  {}%", (int) (props.getJpgQuality() * 100));
        log.info("원본 삭제: {}", props.isDeleteOriginal() ? "예" : "아니오");

        if (props.isProcessExisting()) {
            processExistingFiles(watchPath, outputPath);
        }

        Thread thread = new Thread(() -> watch(watchPath, outputPath));
        thread.setDaemon(true);
        thread.setName("folder-watcher");
        thread.start();
    }

    private void processExistingFiles(Path watchPath, Path outputPath) {
        log.info("기존 PNG 파일 변환 중...");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(watchPath, "*.png")) {
            for (Path png : stream) {
                convert(png, outputPath);
            }
        } catch (IOException e) {
            log.error("기존 파일 처리 중 오류", e);
        }
    }

    private void watch(Path watchPath, Path outputPath) {
        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            watchPath.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);
            log.info("PNG 파일 대기 중...");

            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = watcher.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;

                    Path filename = (Path) event.context();
                    if (filename.toString().toLowerCase().endsWith(".png")) {
                        Path fullPath = watchPath.resolve(filename);
                        // 파일 쓰기가 완전히 끝날 때까지 대기
                        Thread.sleep(500);
                        convert(fullPath, outputPath);
                    }
                }
                if (!key.reset()) break;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            log.error("폴더 감시 중 오류 발생", e);
        }
    }

    private void convert(Path pngPath, Path outputPath) {
        try {
            converterService.convertToJpg(pngPath, outputPath, props.getJpgQuality());
            if (props.isDeleteOriginal()) {
                Files.delete(pngPath);
                log.info("[원본 삭제] {}", pngPath.getFileName());
            }
        } catch (IOException e) {
            log.error("[변환 실패] {}: {}", pngPath.getFileName(), e.getMessage());
        }
    }
}
