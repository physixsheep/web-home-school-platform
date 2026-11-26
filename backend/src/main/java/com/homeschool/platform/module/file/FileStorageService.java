package com.homeschool.platform.module.file;

import com.homeschool.platform.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(StorageProperties properties) {
        this.root = Path.of(properties.getRoot()).toAbsolutePath().normalize();
    }

    public StoredFile store(MultipartFile file, StorageProperties properties) {
        if (file.isEmpty()) {
            throw new BusinessException("不能上传空文件");
        }
        if (file.getSize() > properties.getMaxSizeBytes()) {
            throw new BusinessException("文件大小超过限制");
        }
        String originalName = sanitizeName(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        if (!properties.getAllowedExtensions().contains(extension)) {
            throw new BusinessException("不允许上传该文件类型");
        }

        LocalDate today = LocalDate.now();
        String storageKey = String.format(
                "%d/%02d/%s.%s",
                today.getYear(),
                today.getMonthValue(),
                UUID.randomUUID(),
                extension);
        Path target = resolveSafe(storageKey);
        try {
            Files.createDirectories(target.getParent());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = file.getInputStream();
                 DigestInputStream digestInput = new DigestInputStream(input, digest)) {
                Files.copy(digestInput, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredFile(
                    storageKey,
                    originalName,
                    extension,
                    StringUtils.hasText(file.getContentType())
                            ? file.getContentType()
                            : "application/octet-stream",
                    file.getSize(),
                    HexFormat.of().formatHex(digest.digest()));
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new BusinessException(500, "文件保存失败");
        }
    }

    public Path resolveSafe(String storageKey) {
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(403, "文件路径不合法");
        }
        return target;
    }

    private String sanitizeName(String originalName) {
        if (!StringUtils.hasText(originalName)) {
            throw new BusinessException("文件名不能为空");
        }
        String safe = Path.of(originalName).getFileName().toString();
        if (safe.length() > 255) {
            throw new BusinessException("文件名过长");
        }
        return safe;
    }

    private String extensionOf(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            throw new BusinessException("文件缺少扩展名");
        }
        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    public record StoredFile(
            String storageKey,
            String originalName,
            String extension,
            String mimeType,
            long fileSize,
            String sha256) {
    }
}

