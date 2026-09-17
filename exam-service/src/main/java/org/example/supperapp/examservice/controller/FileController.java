package org.example.supperapp.examservice.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.supperapp.examservice.service.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/file")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class FileController {
    FileService fileService;

    @PostMapping("/import")
    public ResponseEntity<Void> importExam(
            @RequestPart("file") MultipartFile file,
            @RequestParam Integer year,
            @RequestParam Integer testNumber) {
        fileService.uploadFile(file, year, testNumber);
        return ResponseEntity.ok().build();
    }
}
