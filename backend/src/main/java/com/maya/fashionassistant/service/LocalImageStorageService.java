package com.maya.fashionassistant.service;

import com.maya.fashionassistant.exception.BadRequestException;
import com.maya.fashionassistant.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class LocalImageStorageService implements ImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalImageStorageService.class);

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final long MAX_FILE_SIZE = 15 * 1024 * 1024; // 15MB

    private final Path rootLocation;

    public LocalImageStorageService(@Value("${storage.location:./uploads/images}") String storageLocation) {
        this.rootLocation = Paths.get(storageLocation).toAbsolutePath().normalize();
        init();
    }

    private void init() {
        try {
            Files.createDirectories(this.rootLocation);
            Files.createDirectories(this.rootLocation.resolve("original"));
            Files.createDirectories(this.rootLocation.resolve("items"));
            Files.createDirectories(this.rootLocation.resolve("outfits"));
        } catch (IOException e) {
            log.error("Could not initialize storage directories: {}", e.getMessage());
        }
    }

    @Override
    public String store(MultipartFile file, String subDirectory) {
        validateFile(file);

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg"
        );
        String extension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID() + "." + extension;

        try {
            Path targetDir = resolveAndVerifySubDirectory(subDirectory);
            Path destinationFile = targetDir.resolve(uniqueFilename).normalize();

            // Strict path traversal check
            if (!destinationFile.startsWith(this.rootLocation)) {
                throw new BadRequestException("Security violation: path traversal attempt detected");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // Return relative path from rootLocation
            return this.rootLocation.relativize(destinationFile).toString();
        } catch (IOException e) {
            log.error("Failed to store file {}: {}", file.getOriginalFilename(), e.getMessage());
            throw new RuntimeException("Failed to store image: " + e.getMessage(), e);
        }
    }

    @Override
    public String store(InputStream inputStream, String filename, String subDirectory) {
        String extension = getFileExtension(filename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            extension = "png";
        }
        String uniqueFilename = UUID.randomUUID() + "." + extension;

        try {
            Path targetDir = resolveAndVerifySubDirectory(subDirectory);
            Path destinationFile = targetDir.resolve(uniqueFilename).normalize();

            if (!destinationFile.startsWith(this.rootLocation)) {
                throw new BadRequestException("Security violation: path traversal attempt detected");
            }

            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            return this.rootLocation.relativize(destinationFile).toString();
        } catch (IOException e) {
            log.error("Failed to store stream as file: {}", e.getMessage());
            throw new RuntimeException("Failed to store image stream: " + e.getMessage(), e);
        }
    }

    @Override
    public Resource loadAsResource(String storagePath) {
        if (!StringUtils.hasText(storagePath)) {
            throw new ResourceNotFoundException("Storage path is empty");
        }

        try {
            Path file = this.rootLocation.resolve(storagePath).normalize();
            if (!file.startsWith(this.rootLocation)) {
                throw new BadRequestException("Security violation: path traversal attempt");
            }

            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Image file not found: " + storagePath);
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Could not read image file: " + storagePath);
        }
    }

    @Override
    public boolean exists(String storagePath) {
        if (!StringUtils.hasText(storagePath)) return false;
        Path file = this.rootLocation.resolve(storagePath).normalize();
        return file.startsWith(this.rootLocation) && Files.exists(file);
    }

    @Override
    public boolean delete(String storagePath) {
        if (!StringUtils.hasText(storagePath)) return false;
        try {
            Path file = this.rootLocation.resolve(storagePath).normalize();
            if (file.startsWith(this.rootLocation)) {
                return Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            log.warn("Failed to delete file {}: {}", storagePath, e.getMessage());
        }
        return false;
    }

    private Path resolveAndVerifySubDirectory(String subDirectory) throws IOException {
        Path targetDir = this.rootLocation;
        if (StringUtils.hasText(subDirectory)) {
            targetDir = this.rootLocation.resolve(subDirectory).normalize();
            if (!targetDir.startsWith(this.rootLocation)) {
                throw new BadRequestException("Invalid storage subdirectory");
            }
        }
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }
        return targetDir;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds 15MB limit");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Invalid file MIME type. Only JPEG, PNG, and WEBP images are supported.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = getFileExtension(originalFilename).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new BadRequestException("Invalid file extension: ." + extension);
            }
            if (originalFilename.contains("..")) {
                throw new BadRequestException("Filename cannot contain path traversal sequences");
            }
        }

        // Validate image content integrity
        try (InputStream is = file.getInputStream()) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                throw new BadRequestException("Uploaded file is not a valid or readable image");
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to verify image content: " + e.getMessage());
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
