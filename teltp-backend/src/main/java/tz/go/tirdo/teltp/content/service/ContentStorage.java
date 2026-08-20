package tz.go.tirdo.teltp.content.service;

/**
 * Storage seam. v1 ships a filesystem implementation; an S3 implementation can be
 * dropped in behind this interface (teltp.content.storage.backend) without touching callers.
 */
public interface ContentStorage {
    /** Persist bytes and return the opaque storage key. */
    String store(String suggestedName, byte[] data, String mimeType);

    byte[] retrieve(String storageKey);

    void delete(String storageKey);
}
