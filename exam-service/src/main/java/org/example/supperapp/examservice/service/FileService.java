package org.example.supperapp.examservice.service;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.example.supperapp.examservice.entity.Exam;
import org.example.supperapp.examservice.exception.PdfImportException;
import org.example.supperapp.examservice.repository.ExamRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService {

    private final ExamRepository examRepository;
    private final ToeicPdfParser toeicPdfParser;

    public void uploadFile(MultipartFile file, Integer year, Integer testNumber) {
        validateRequest(file, year, testNumber);

        try (InputStream input = file.getInputStream();
                RandomAccessReadBuffer pdfBuffer = new RandomAccessReadBuffer(input);
                PDDocument document = Loader.loadPDF(pdfBuffer)) {
            if (document.isEncrypted()) {
                throw new PdfImportException("PDF is password-protected and cannot be imported");
            }

            List<Exam> parsedParts = toeicPdfParser.parse(document, year, testNumber);

            // Do not persist anything until every expected question has been parsed and validated.
            examRepository.saveAll(parsedParts);
        } catch (PdfImportException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new PdfImportException("The uploaded file is not a readable PDF", exception);
        }
    }

    private void validateRequest(MultipartFile file, Integer year, Integer testNumber) {
        if (file == null || file.isEmpty()) {
            throw new PdfImportException("PDF file must not be empty");
        }
        if (year == null || year < 2000 || year > 2100) {
            throw new PdfImportException("year must be between 2000 and 2100");
        }
        if (testNumber == null || testNumber < 1) {
            throw new PdfImportException("testNumber must be greater than zero");
        }
    }
}
