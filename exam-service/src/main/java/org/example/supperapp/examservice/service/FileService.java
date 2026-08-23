package org.example.supperapp.examservice.service;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.example.supperapp.examservice.entity.*;
import org.example.supperapp.examservice.entity.enumeric.PartType;
import org.example.supperapp.examservice.repository.ExamRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FileService {

    private final ExamRepository examRepository;

    private static final Pattern QUESTION_PATTERN = Pattern.compile(
            "(\\d+)\\.\\s*(.*?)\\(A\\)\\s*(.*?)\\(B\\)\\s*(.*?)\\(C\\)\\s*(.*?)\\(D\\)\\s*(.*?)(?=\\d+\\.|$)",
            Pattern.DOTALL
    );

    public void uploadFile(MultipartFile file,
                           Integer year,
                           Integer testNumber) throws IOException {

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {

            String pdfText = new PDFTextStripper().getText(document);

            examRepository.save(parsePart1(document, year, testNumber));
            examRepository.save(parsePart2(year, testNumber));
            examRepository.save(parsePart3(
                    extractPart(pdfText, "PART 3", "PART 4"),
                    year,
                    testNumber
            ));
            examRepository.save(parsePart4(
                    extractPart(pdfText, "PART 4", null),
                    year,
                    testNumber
            ));
        }
    }

    /* ======================================================
                        PART 1
       ====================================================== */

    private Exam parsePart1(PDDocument document,
                            Integer year,
                            Integer testNumber) throws IOException {

        PDFRenderer renderer = new PDFRenderer(document);

        List<Question> questions = new ArrayList<>();

        int questionNumber = 1;

        // ETS Part 1 ở trang 2,3,4 (index 1,2,3)
        for (int page = 1; page <= 3; page++) {

            BufferedImage pageImage = renderer.renderImageWithDPI(page, 180);

            int width = pageImage.getWidth();
            int height = pageImage.getHeight();

            BufferedImage top =
                    pageImage.getSubimage(0, 0, width, height / 2);

            BufferedImage bottom =
                    pageImage.getSubimage(0, height / 2, width, height / 2);

            questions.add(buildImageQuestion(questionNumber++, top));
            questions.add(buildImageQuestion(questionNumber++, bottom));
        }

        return Exam.builder()
                .year(year)
                .testNumber(testNumber)
                .partNumber(1)
                .type(PartType.LISTENING)
                .title("Photographs")
                .questions(questions)
                .groups(null)
                .build();
    }

    private Question buildImageQuestion(int number,
                                        BufferedImage image) throws IOException {

        return Question.builder()
                .questionNumber(number)
                .content(null)
                .imageBase64(toCompressedBase64(image))
                .options(new ArrayList<>())
                .build();
    }

    /* ======================================================
                        PART 2
       ====================================================== */

    private Exam parsePart2(Integer year,
                            Integer testNumber) {

        List<Question> questions = new ArrayList<>();

        for (int i = 7; i <= 31; i++) {

            questions.add(
                    Question.builder()
                            .questionNumber(i)
                            .content(null)
                            .options(new ArrayList<>())
                            .build()
            );
        }

        return Exam.builder()
                .year(year)
                .testNumber(testNumber)
                .partNumber(2)
                .type(PartType.LISTENING)
                .title("Question Response")
                .questions(questions)
                .groups(null)
                .build();
    }

    /* ======================================================
                        PART 3
       ====================================================== */

    private Exam parsePart3(String text,
                            Integer year,
                            Integer testNumber) {

        return Exam.builder()
                .year(year)
                .testNumber(testNumber)
                .partNumber(3)
                .type(PartType.LISTENING)
                .title("Conversations")
                .questions(null)
                .groups(buildGroups(text, 32, 70))
                .build();
    }

    /* ======================================================
                        PART 4
       ====================================================== */

    private Exam parsePart4(String text,
                            Integer year,
                            Integer testNumber) {

        return Exam.builder()
                .year(year)
                .testNumber(testNumber)
                .partNumber(4)
                .type(PartType.LISTENING)
                .title("Talks")
                .questions(null)
                .groups(buildGroups(text, 71, 100))
                .build();
    }

    /* ======================================================
                        GROUP
       ====================================================== */

    private List<QuestionGroup> buildGroups(String text,
                                            int startQuestion,
                                            int endQuestion) {

        List<Question> questions =
                parseQuestions(text, startQuestion, endQuestion);

        List<QuestionGroup> groups = new ArrayList<>();

        int groupNumber = 1;

        for (int i = 0; i < questions.size(); i += 3) {

            groups.add(
                    QuestionGroup.builder()
                            .groupNumber(groupNumber++)
                            .audioUrl(null)
                            .passage(null)
                            .questions(
                                    questions.subList(
                                            i,
                                            Math.min(i + 3, questions.size())
                                    )
                            )
                            .build()
            );
        }

        return groups;
    }

    /* ======================================================
                        QUESTION PARSER
       ====================================================== */

    private List<Question> parseQuestions(String text,
                                          int startQuestion,
                                          int endQuestion) {

        Matcher matcher = QUESTION_PATTERN.matcher(text);

        List<Question> questions = new ArrayList<>();

        while (matcher.find()) {

            int number = Integer.parseInt(matcher.group(1));

            if (number < startQuestion || number > endQuestion)
                continue;

            questions.add(
                    Question.builder()
                            .questionNumber(number)
                            .content(clean(matcher.group(2)))
                            .options(List.of(
                                    option("A", matcher.group(3)),
                                    option("B", matcher.group(4)),
                                    option("C", matcher.group(5)),
                                    option("D", matcher.group(6))
                            ))
                            .build()
            );
        }

        return questions;
    }

    /* ======================================================
                        IMAGE
       ====================================================== */

    private String toCompressedBase64(BufferedImage image)
            throws IOException {

        BufferedImage resized = resize(image, 600);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Iterator<ImageWriter> writers =
                ImageIO.getImageWritersByFormatName("jpg");

        ImageWriter writer = writers.next();

        ImageOutputStream ios =
                ImageIO.createImageOutputStream(out);

        writer.setOutput(ios);

        ImageWriteParam param =
                writer.getDefaultWriteParam();

        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.6f);

        writer.write(
                null,
                new IIOImage(resized, null, null),
                param
        );

        ios.close();
        writer.dispose();

        return Base64.getEncoder()
                .encodeToString(out.toByteArray());
    }

    private BufferedImage resize(BufferedImage original,
                                 int targetWidth) {

        int targetHeight =
                original.getHeight() * targetWidth / original.getWidth();

        BufferedImage resized =
                new BufferedImage(
                        targetWidth,
                        targetHeight,
                        BufferedImage.TYPE_INT_RGB
                );

        Graphics2D g = resized.createGraphics();

        g.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
        );

        g.drawImage(
                original,
                0,
                0,
                targetWidth,
                targetHeight,
                null
        );

        g.dispose();

        return resized;
    }

    /* ======================================================
                        HELPERS
       ====================================================== */

    private String extractPart(String text,
                               String startKey,
                               String endKey) {

        int start = text.indexOf(startKey);

        if (start == -1)
            return "";

        int end = endKey == null
                ? text.length()
                : text.indexOf(endKey);

        if (end == -1)
            end = text.length();

        return text.substring(start, end);
    }

    private Option option(String label,
                          String value) {

        return Option.builder()
                .label(label)
                .text(clean(value))
                .build();
    }

    private String clean(String value) {

        return value
                .replaceAll("\\s+", " ")
                .trim();
    }
}