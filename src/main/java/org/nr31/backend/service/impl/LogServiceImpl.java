package org.nr31.backend.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.nr31.backend.exception.ElementNotFoundException;
import org.nr31.backend.exception.ResourceAccessException;
import org.nr31.backend.service.LogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@Slf4j
@Service
public class LogServiceImpl implements LogService {

    private final String logDirectory;

    public LogServiceImpl(@Value("${app.logs.dir:/app/logs}") String logDirectory) {
        this.logDirectory = logDirectory;
    }

    @Override
    public Page<String> listLogFiles(Pageable pageable) {
        try (Stream<Path> paths = Files.walk(Paths.get(logDirectory), 1)) {
            List<String> allFiles = paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".log"))
                    .collect(Collectors.toList());

            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), allFiles.size());

            if (start > allFiles.size()) {
                return new PageImpl<>(Collections.emptyList(), pageable, allFiles.size());
            }

            return new PageImpl<>(allFiles.subList(start, end), pageable, allFiles.size());
        } catch (Exception e) {
            throw new ResourceAccessException("Unable to list log files", e);
        }
    }

    @Override
    public Resource getLogFile(String fileName, Long offsetFromEnd, Long limit) {
        Path baseDirectory = Paths.get(logDirectory).toAbsolutePath().normalize();
        Path filePath = baseDirectory.resolve(fileName).normalize();

        if (!filePath.startsWith(baseDirectory)) {
            throw new AuthorizationDeniedException("Invalid path access: " + fileName);
        }

        File logFile = new File(logDirectory, fileName);

        if (!logFile.exists() || !logFile.isFile()) {
            throw new ElementNotFoundException("Log file not found: " + fileName);
        }

        if (offsetFromEnd != null || limit != null) {
            long off = offsetFromEnd != null ? offsetFromEnd : 0;
            long lim = limit != null ? limit : 100;
            List<String> lines = new ArrayList<>();
            try (ReversedLinesFileReader reader = ReversedLinesFileReader.builder()
                    .setFile(logFile)
                    .setCharset(StandardCharsets.UTF_8)
                    .setBufferSize(4096)
                    .get()) {
                for (long i = 0; i < off; i++) {
                    if (reader.readLine() == null)
                        break;
                }
                for (long i = 0; i < lim; i++) {
                    String line = reader.readLine();
                    if (line == null)
                        break;
                    lines.add(line);
                }
            } catch (IOException e) {
                log.error("Error reading log file", e);
                throw new ResourceAccessException("Error reading log file: " + fileName, e);
            }
            Collections.reverse(lines);
            String content = lines.isEmpty() ? "" : String.join("\n", lines) + "\n";
            return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
        }

        return new FileSystemResource(logFile);
    }
}
