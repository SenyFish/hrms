package com.hrms.service;

import com.hrms.config.UploadProperties;
import com.hrms.entity.SysFile;
import com.hrms.repository.SysFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final UploadProperties uploadProperties;
    private final SysFileRepository sysFileRepository;

    public SysFile store(MultipartFile file, Long uploaderId) throws IOException {
        Path root = Paths.get(uploadProperties.getUploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(root);
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            ext = original.substring(dot);
        }
        String stored = UUID.randomUUID() + ext;
        Path target = root.resolve(stored);
        file.transferTo(target.toFile());

        SysFile sf = new SysFile();
        sf.setOriginalName(original);
        sf.setStoredName(stored);
        sf.setRelativePath(stored);
        sf.setSizeBytes(file.getSize());
        sf.setUploaderId(uploaderId);
        return sysFileRepository.save(sf);
    }

    public Resource loadAsResource(Long id) throws IOException {
        SysFile sf = sysFileRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("文件不存在"));
        Path root = Paths.get(uploadProperties.getUploadDir()).toAbsolutePath().normalize();
        Path file = root.resolve(sf.getRelativePath());
        if (!Files.exists(file)) {
            throw new IllegalArgumentException("文件已丢失");
        }
        return new UrlResource(file.toUri());
    }

    public SysFile getMeta(Long id) {
        return sysFileRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("文件不存在"));
    }
}
