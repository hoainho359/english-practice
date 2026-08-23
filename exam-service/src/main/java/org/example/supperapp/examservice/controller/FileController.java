package org.example.supperapp.examservice.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.supperapp.examservice.dto.response.ApiResponse;
import org.example.supperapp.examservice.entity.Exam;
import org.example.supperapp.examservice.service.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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
            @RequestParam Integer testNumber) throws IOException {

        fileService.uploadFile(file, year, testNumber);

        return ResponseEntity.ok().build();
    }
}
