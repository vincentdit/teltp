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
            Path dir = Path.of(basePath).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String safe = suggestedName.replaceAll("[^a-zA-Z0-9._-]", "_");
            if (safe.isBlank() || safe.equals(".") || safe.equals("..")) safe = "file";
            String key = UUID.randomUUID() + "-" + safe;
            Path target = dir.resolve(key).normalize();
            if (!target.startsWith(dir)) throw new BusinessRuleException("Invalid storage name");
            Files.write(target, data, StandardOpenOption.CREATE_NEW);
            return key;
        } catch (IOException e) {
            throw new BusinessRuleException("Failed to store content: " + e.getMessage());
        }
    }

    @Override
    public byte[] retrieve(String storageKey) {
        try {
            Path base = Path.of(basePath).toAbsolutePath().normalize();
            Path target = base.resolve(storageKey).normalize();
            if (!target.startsWith(base)) throw new BusinessRuleException("Invalid storage key");
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new BusinessRuleException("Failed to read content: " + e.getMessage());
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Path base = Path.of(basePath).toAbsolutePath().normalize();
            Path target = base.resolve(storageKey).normalize();
            if (!target.startsWith(base)) throw new BusinessRuleException("Invalid storage key");
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new BusinessRuleException("Failed to delete content: " + e.getMessage());
        }
    }
}
