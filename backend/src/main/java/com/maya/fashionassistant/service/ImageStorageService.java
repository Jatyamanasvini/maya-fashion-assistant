package com.maya.fashionassistant.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Path;

public interface ImageStorageService {

    /**
     * Stores an uploaded multipart file securely and returns the relative storage path or key.
     */
    String store(MultipartFile file, String subDirectory);

    /**
     * Stores an input stream securely (e.g. For cropped images or generated images).
     */
    String store(InputStream inputStream, String filename, String subDirectory);

    /**
     * Loads the stored image as a Spring Resource.
     */
    Resource loadAsResource(String storagePath);

    /**
     * Checks if an image exists in storage.
     */
    boolean exists(String storagePath);

    /**
     * Deletes an image from storage.
     */
    boolean delete(String storagePath);
}
