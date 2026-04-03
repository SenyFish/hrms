package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.entity.SysFile;
import com.hrms.repository.SysFileRepository;
import com.hrms.security.LoginUser;
import com.hrms.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;
    private final SysFileRepository sysFileRepository;

    @GetMapping
    public ApiResponse<List<SysFile>> list(@AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        return ApiResponse.ok(sysFileRepository.findAll());
    }

    @PostMapping("/upload")
    public ApiResponse<SysFile> upload(@RequestParam("file") MultipartFile file,
                                       @AuthenticationPrincipal LoginUser loginUser) throws IOException {
        if (loginUser == null) {
            throw new AccessDeniedException("未登录");
        }
        return ApiResponse.ok(fileStorageService.store(file, loginUser.getUserId()));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser)
            throws IOException {
        if (loginUser == null) {
            throw new AccessDeniedException("未登录");
        }
        SysFile meta = fileStorageService.getMeta(id);
        Resource resource = fileStorageService.loadAsResource(id);
        String encoded = URLEncoder.encode(meta.getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    private static boolean isAdmin(LoginUser u) {
        return u != null && "ADMIN".equals(u.getRoleCode());
    }

    private static boolean isHr(LoginUser u) {
        return u != null && "HR".equals(u.getRoleCode());
    }

    private static void assertAdminOrHr(LoginUser u) {
        if (!isAdmin(u) && !isHr(u)) {
            throw new AccessDeniedException("需要管理员或人事权限");
        }
    }
}
