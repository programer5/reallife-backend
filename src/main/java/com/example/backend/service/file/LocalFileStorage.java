package com.example.backend.service.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.*;

@Component
public class LocalFileStorage {

    private final Path rootDir;

    public LocalFileStorage(@Value("${file.upload-dir:uploads}") String uploadDir) throws IOException {
        this.rootDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.rootDir);
    }

    public String store(String storedFilename, byte[] bytes) throws IOException {
        if (!StringUtils.hasText(storedFilename)) throw new IllegalArgumentException("storedFilename required");
        Path target = rootDir.resolve(storedFilename);
        Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
        return target.toString();
    }

    public Path load(String storedFilename) {
        return rootDir.resolve(storedFilename);
    }
}