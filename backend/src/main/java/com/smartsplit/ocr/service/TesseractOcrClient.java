package com.smartsplit.ocr.service;

import com.smartsplit.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class TesseractOcrClient implements ReceiptOcrClient {
    private static final String PROVIDER = "TESSERACT_LOCAL";
    private static final Pattern MONEY_HINT = Pattern.compile(
            "(?<!\\d)(?:\\d{1,3}(?:[.,]\\d{3})+|\\d{4,10})(?!\\d)"
    );
    private static final Pattern DATE_HINT = Pattern.compile(
            "(?<!\\d)\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4}(?!\\d)"
    );

    private final boolean enabled;
    private final String configuredExecutable;
    private final String dataPath;
    private final String language;
    private final int pageSegmentationMode;
    private final boolean multiPass;
    private final Duration timeout;
    private final ReceiptImagePreprocessor preprocessor;

    private volatile ConfigurationState configurationState;

    public TesseractOcrClient(
            @Value("${app.ocr.enabled:true}") boolean enabled,
            @Value("${app.ocr.tesseract.executable:tesseract}") String executable,
            @Value("${app.ocr.tesseract.data-path:}") String dataPath,
            @Value("${app.ocr.tesseract.language:vie+eng}") String language,
            @Value("${app.ocr.tesseract.page-segmentation-mode:6}") int pageSegmentationMode,
            @Value("${app.ocr.tesseract.multi-pass:true}") boolean multiPass,
            @Value("${app.ocr.tesseract.timeout-seconds:40}") long timeoutSeconds,
            ReceiptImagePreprocessor preprocessor
    ) {
        this.enabled = enabled;
        this.configuredExecutable = executable == null ? "tesseract" : executable.trim();
        this.dataPath = dataPath == null ? "" : dataPath.trim();
        this.language = language == null || language.isBlank() ? "vie+eng" : language.trim();
        this.pageSegmentationMode = Math.max(3, Math.min(pageSegmentationMode, 13));
        this.multiPass = multiPass;
        this.timeout = Duration.ofSeconds(Math.max(5L, Math.min(timeoutSeconds, 180L)));
        this.preprocessor = preprocessor;
    }

    @Override
    public boolean isConfigured() {
        return configuration().configured();
    }

    @Override
    public String configurationMessage() {
        return configuration().message();
    }

    @Override
    public OcrTextResult extractText(Path imagePath) {
        ConfigurationState state = configuration();
        if (!state.configured()) {
            throw new BusinessException("OCR_NOT_CONFIGURED", state.message());
        }
        if (imagePath == null || !Files.isRegularFile(imagePath)) {
            throw new BusinessException("RECEIPT_FILE_NOT_FOUND", "Không tìm thấy ảnh hóa đơn", HttpStatus.NOT_FOUND);
        }

        try (PreparedReceiptImage prepared = preprocessor.prepare(imagePath)) {
            List<Integer> modes = pageSegmentationModes();
            List<OcrPassResult> results = new ArrayList<>();
            for (int mode : modes) {
                try {
                    results.add(runOcrPass(state.executable(), prepared.path(), mode));
                } catch (BusinessException exception) {
                    if (modes.size() == 1) throw exception;
                    // A secondary page segmentation mode may fail for a particular image.
                }
            }

            OcrPassResult best = results.stream()
                    .filter(result -> !result.text().isBlank())
                    .max(Comparator.comparingInt(OcrPassResult::qualityScore))
                    .orElseThrow(() -> new BusinessException(
                            "OCR_TEXT_NOT_FOUND",
                            "Không nhận dạng được chữ trong ảnh. Hãy chụp thẳng, đủ sáng và rõ nét."
                    ));
            return new OcrTextResult(PROVIDER, best.text());
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw providerFailure(exception.getMessage());
        }
    }

    private OcrPassResult runOcrPass(String executable, Path input, int psm) {
        Path outputDirectory = null;
        try {
            outputDirectory = Files.createTempDirectory("smartsplit-tesseract-");
            Path outputBase = outputDirectory.resolve("result");
            Path outputText = outputDirectory.resolve("result.txt");
            Path processLog = outputDirectory.resolve("tesseract.log");

            List<String> command = buildOcrCommand(executable, input, outputBase, psm);
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(processLog.toFile())
                    .start();
            boolean finished = process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw providerFailure("Tesseract xử lý quá thời gian " + timeout.toSeconds() + " giây");
            }

            String log = readQuietly(processLog).trim();
            if (process.exitValue() != 0) {
                throw providerFailure(cleanLog(log));
            }
            if (!Files.isRegularFile(outputText)) {
                throw providerFailure("Tesseract không tạo tệp kết quả");
            }

            String text = Files.readString(outputText, StandardCharsets.UTF_8).trim();
            return new OcrPassResult(psm, text, scoreText(text));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw providerFailure("Tiến trình OCR bị gián đoạn");
        } catch (IOException exception) {
            throw providerFailure(exception.getMessage());
        } finally {
            deleteTreeQuietly(outputDirectory);
        }
    }

    private List<Integer> pageSegmentationModes() {
        Set<Integer> modes = new LinkedHashSet<>();
        modes.add(pageSegmentationMode);
        if (multiPass) {
            // PSM 11 works well for sparse bank/app screenshots; PSM 4 helps receipt columns.
            modes.add(11);
            modes.add(4);
        }
        return List.copyOf(modes);
    }

    private int scoreText(String text) {
        if (text == null || text.isBlank()) return Integer.MIN_VALUE;
        String lower = text.toLowerCase(Locale.ROOT);
        int score = Math.min(80, text.length() / 18);
        score += Math.min(60, (int) text.lines().filter(line -> !line.isBlank()).count() * 3);
        score += countMatches(MONEY_HINT, text) * 14;
        score += countMatches(DATE_HINT, text) * 10;
        for (String keyword : List.of(
                "tổng", "tong", "total", "số tiền", "so tien", "amount", "thanh toán",
                "thanh toan", "người nhận", "nguoi nhan", "merchant", "cửa hàng", "cua hang"
        )) {
            if (lower.contains(keyword)) score += 18;
        }
        long replacementCharacters = text.chars().filter(character -> character == 0xfffd).count();
        score -= (int) replacementCharacters * 15;
        return score;
    }

    private int countMatches(Pattern pattern, String value) {
        int count = 0;
        var matcher = pattern.matcher(value);
        while (matcher.find()) count++;
        return count;
    }

    private ConfigurationState configuration() {
        ConfigurationState current = configurationState;
        if (current != null) return current;
        synchronized (this) {
            if (configurationState == null) configurationState = detectConfiguration();
            return configurationState;
        }
    }

    private ConfigurationState detectConfiguration() {
        if (!enabled) {
            return new ConfigurationState(
                    false,
                    configuredExecutable,
                    "OCR đang tắt. Đặt OCR_ENABLED=true để dùng Tesseract local."
            );
        }

        String executable = resolveExecutable();
        Path log = null;
        try {
            log = Files.createTempFile("smartsplit-tesseract-version-", ".log");
            Process process = new ProcessBuilder(executable, "--version")
                    .redirectErrorStream(true)
                    .redirectOutput(log.toFile())
                    .start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return notConfigured(executable, "Không thể kiểm tra phiên bản Tesseract");
            }
            if (process.exitValue() != 0) {
                return notConfigured(executable, cleanLog(readQuietly(log)));
            }

            String languageError = validateLanguages(executable);
            if (languageError != null) return notConfigured(executable, languageError);
            return new ConfigurationState(true, executable, "Tesseract local đã sẵn sàng");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return notConfigured(executable, "Quá trình kiểm tra Tesseract bị gián đoạn");
        } catch (IOException | RuntimeException exception) {
            return notConfigured(executable, exception.getMessage());
        } finally {
            deleteQuietly(log);
        }
    }

    private String validateLanguages(String executable) {
        Path log = null;
        try {
            log = Files.createTempFile("smartsplit-tesseract-langs-", ".log");
            List<String> command = new ArrayList<>();
            command.add(executable);
            if (!dataPath.isBlank()) {
                command.add("--tessdata-dir");
                command.add(dataPath);
            }
            command.add("--list-langs");

            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(log.toFile())
                    .start();
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return "Không thể đọc danh sách ngôn ngữ Tesseract";
            }
            String output = readQuietly(log).toLowerCase(Locale.ROOT);
            if (process.exitValue() != 0) return cleanLog(output);
            for (String required : language.split("\\+")) {
                String code = required.trim().toLowerCase(Locale.ROOT);
                if (!code.isBlank() && !output.lines().map(String::trim).anyMatch(code::equals)) {
                    return "Thiếu dữ liệu ngôn ngữ '" + code
                            + ".traineddata'. Hãy cài bộ ngôn ngữ vie và eng trong thư mục tessdata.";
                }
            }
            return null;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return "Quá trình kiểm tra ngôn ngữ Tesseract bị gián đoạn";
        } catch (IOException exception) {
            return exception.getMessage();
        } finally {
            deleteQuietly(log);
        }
    }

    private List<String> buildOcrCommand(String executable, Path input, Path outputBase, int psm) {
        List<String> command = new ArrayList<>();
        command.add(executable);
        command.add(input.toAbsolutePath().toString());
        command.add(outputBase.toAbsolutePath().toString());
        if (!dataPath.isBlank()) {
            command.add("--tessdata-dir");
            command.add(dataPath);
        }
        command.add("-l");
        command.add(language);
        command.add("--oem");
        command.add("1");
        command.add("--psm");
        command.add(String.valueOf(psm));
        command.add("--dpi");
        command.add("300");
        command.add("-c");
        command.add("preserve_interword_spaces=1");
        return command;
    }

    private String resolveExecutable() {
        if (!configuredExecutable.isBlank() && !"tesseract".equalsIgnoreCase(configuredExecutable)) {
            return configuredExecutable;
        }
        Path windowsDefault = Path.of("C:/Program Files/Tesseract-OCR/tesseract.exe");
        if (Files.isRegularFile(windowsDefault)) return windowsDefault.toString();
        return configuredExecutable.isBlank() ? "tesseract" : configuredExecutable;
    }

    private ConfigurationState notConfigured(String executable, String detail) {
        String reason = detail == null || detail.isBlank() ? "không tìm thấy chương trình" : detail;
        return new ConfigurationState(
                false,
                executable,
                "Chưa dùng được Tesseract local: " + reason
                        + ". Hãy cài Tesseract 5 và bộ ngôn ngữ vie+eng, rồi kiểm tra bằng 'tesseract --version'."
        );
    }

    private BusinessException providerFailure(String detail) {
        String reason = detail == null || detail.isBlank() ? "lỗi không xác định" : detail;
        return new BusinessException(
                "OCR_PROVIDER_FAILED",
                "Tesseract local xử lý thất bại: " + reason,
                HttpStatus.BAD_GATEWAY
        );
    }

    private String readQuietly(Path path) {
        if (path == null || !Files.exists(path)) return "";
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return "";
        }
    }

    private String cleanLog(String value) {
        if (value == null || value.isBlank()) return "Tesseract trả về lỗi nhưng không có thông tin chi tiết";
        String compact = value.replaceAll("\\s+", " ").trim();
        return compact.length() > 420 ? compact.substring(0, 420) : compact;
    }

    private void deleteQuietly(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Temporary files are safe to leave for later OS cleanup.
        }
    }

    private void deleteTreeQuietly(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(this::deleteQuietly);
        } catch (IOException ignored) {
            // Temporary files are safe to leave for later OS cleanup.
        }
    }

    private record ConfigurationState(boolean configured, String executable, String message) {
    }

    private record OcrPassResult(int psm, String text, int qualityScore) {
    }
}
