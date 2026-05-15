package ru.baikalsr.backend.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import ru.baikalsr.backend.common.dto.LogChunkDto;
import ru.baikalsr.backend.common.dto.LogLineDto;
import ru.baikalsr.backend.common.dto.LogSearchResultDto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogViewerService {

    private static final int MAX_LIMIT = 500;
    private static final int DEFAULT_AROUND_BEFORE = 40;
    private static final int DEFAULT_AROUND_AFTER = 60;

    @Value("${logging.file.name:./logs/mdm-backend.log}")
    private String activeLogFilename;

    public LogChunkDto readLatest(int limit) {
        List<String> lines = readAllLines();
        int safeLimit = clampLimit(limit);
        int total = lines.size();
        int startIndex = Math.max(0, total - safeLimit);
        return toChunk(lines, startIndex, total);
    }

    public LogChunkDto readOlder(long beforeLine, int limit) {
        List<String> lines = readAllLines();
        int safeLimit = clampLimit(limit);
        int total = lines.size();
        int exclusiveEnd = clampLineExclusive(beforeLine, total);
        int startIndex = Math.max(0, exclusiveEnd - safeLimit);
        return toChunk(lines, startIndex, exclusiveEnd);
    }

    public LogChunkDto readNewer(long afterLine, int limit) {
        List<String> lines = readAllLines();
        int safeLimit = clampLimit(limit);
        int total = lines.size();
        int startIndex = clampLineInclusive(afterLine + 1, total);
        int exclusiveEnd = Math.min(total, startIndex + safeLimit);
        return toChunk(lines, startIndex, exclusiveEnd);
    }

    public LogChunkDto readAround(long lineNumber, Integer before, Integer after) {
        List<String> lines = readAllLines();
        int total = lines.size();
        if (total == 0) {
            return new LogChunkDto(0, false, false, List.of());
        }

        int beforeCount = before != null ? Math.max(0, before) : DEFAULT_AROUND_BEFORE;
        int afterCount = after != null ? Math.max(0, after) : DEFAULT_AROUND_AFTER;
        int targetIndex = clampLineInclusive(lineNumber, total);
        int startIndex = Math.max(0, targetIndex - beforeCount - 1);
        int exclusiveEnd = Math.min(total, targetIndex + afterCount);
        return toChunk(lines, startIndex, exclusiveEnd);
    }

    public LogSearchResultDto search(String query, int limit) {
        if (!StringUtils.hasText(query)) {
            return new LogSearchResultDto("", 0, false, List.of());
        }

        List<String> lines = readAllLines();
        int safeLimit = clampLimit(limit);
        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        List<Long> matches = new ArrayList<>();
        long totalMatches = 0;

        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                totalMatches++;
                if (matches.size() < safeLimit) {
                    matches.add((long) i + 1);
                }
            }
        }

        return new LogSearchResultDto(query, totalMatches, totalMatches > matches.size(), matches);
    }

    private LogChunkDto toChunk(List<String> lines, int startIndex, int exclusiveEnd) {
        int total = lines.size();
        if (startIndex < 0 || exclusiveEnd < startIndex || exclusiveEnd > total) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_LOG_CHUNK_RANGE");
        }

        List<LogLineDto> chunk = new ArrayList<>(Math.max(0, exclusiveEnd - startIndex));
        for (int i = startIndex; i < exclusiveEnd; i++) {
            chunk.add(new LogLineDto((long) i + 1, lines.get(i)));
        }

        return new LogChunkDto(
                total,
                startIndex > 0,
                exclusiveEnd < total,
                chunk
        );
    }

    private List<String> readAllLines() {
        List<Path> files = resolveLogFiles();
        List<String> result = new ArrayList<>();

        for (Path file : files) {
            result.addAll(readLines(file));
        }

        return result;
    }

    private List<Path> resolveLogFiles() {
        Path activeLogPath = Path.of(activeLogFilename).toAbsolutePath().normalize();
        Path directory = activeLogPath.getParent();
        if (directory == null || !Files.exists(directory)) {
            return List.of();
        }

        String activeName = activeLogPath.getFileName().toString();
        String prefix = activeName.endsWith(".log")
                ? activeName.substring(0, activeName.length() - 4)
                : activeName;

        try (Stream<Path> stream = Files.list(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> matchesLogFile(path, prefix))
                    .sorted(logFileComparator(activeLogPath))
                    .toList();
        } catch (IOException e) {
            log.warn("Failed to list log files in {}", directory, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "LOG_VIEWER_READ_FAILED");
        }
    }

    private boolean matchesLogFile(Path path, String prefix) {
        String filename = path.getFileName().toString();
        return filename.startsWith(prefix) && (filename.endsWith(".log") || filename.endsWith(".gz"));
    }

    private Comparator<Path> logFileComparator(Path activeLogPath) {
        return (left, right) -> {
            boolean leftIsActive = left.toAbsolutePath().normalize().equals(activeLogPath);
            boolean rightIsActive = right.toAbsolutePath().normalize().equals(activeLogPath);
            if (leftIsActive && !rightIsActive) {
                return 1;
            }
            if (!leftIsActive && rightIsActive) {
                return -1;
            }

            try {
                int byTime = Files.getLastModifiedTime(left).compareTo(Files.getLastModifiedTime(right));
                if (byTime != 0) {
                    return byTime;
                }
            } catch (IOException ignored) {
                // fall back to filename ordering below
            }

            return left.getFileName().toString().compareTo(right.getFileName().toString());
        };
    }

    private List<String> readLines(Path path) {
        try (InputStream raw = Files.newInputStream(path);
             InputStream input = path.getFileName().toString().endsWith(".gz")
                     ? new GZIPInputStream(raw)
                     : raw;
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            return reader.lines().toList();
        } catch (IOException e) {
            log.warn("Failed to read log file {}", path, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "LOG_VIEWER_READ_FAILED");
        }
    }

    private int clampLimit(int requested) {
        if (requested <= 0) {
            return 100;
        }
        return Math.min(requested, MAX_LIMIT);
    }

    private int clampLineExclusive(long beforeLine, int total) {
        if (beforeLine <= 1) {
            return 0;
        }
        return (int) Math.min(beforeLine - 1, total);
    }

    private int clampLineInclusive(long lineNumber, int total) {
        if (total <= 0) {
            return 0;
        }
        if (lineNumber <= 1) {
            return 1;
        }
        return (int) Math.min(lineNumber, total);
    }
}
