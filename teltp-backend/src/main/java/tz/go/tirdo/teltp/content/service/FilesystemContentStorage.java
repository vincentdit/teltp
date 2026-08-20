package tz.go.tirdo.teltp.content.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tz.go.tirdo.teltp.common.exception.BusinessRuleException;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FilesystemContentStorage implements ContentStorage {

    @Value("${teltp.content.storage.base-path:/var/teltp/content}")
    private String basePath;

    @Override
    public String store(String suggestedName, byte[] data, String mimeType) {
        try {
            Path dir = Path.of(basePath);
            Files.createDirectories(dir);
            String key = UUID.randomUUID() + "-" + suggestedName.replaceAll("[^a-zA-Z0-9._-]", "_");
            Files.write(dir.resolve(key), data, StandardOpenOption.CREATE_NEW);
            return key;
        } catch (IOException e) {
            throw new BusinessRuleException("Failed to store content: " + e.getMessage());
        }
    }

    @Override
    public byte[] retrieve(String storageKey) {
        try {
            return Files.readAllBytes(Path.of(basePath).resolve(storageKey));
        } catch (IOException e) {
            throw new BusinessRuleException("Failed to read content: " + e.getMessage());
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(Path.of(basePath).resolve(storageKey));
        } catch (IOException e) {
            throw new BusinessRuleException("Failed to delete content: " + e.getMessage());
        }
    }
}
