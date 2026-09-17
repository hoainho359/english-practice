package org.example.supperapp.examservice.service;

import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.example.supperapp.examservice.entity.Exam;
import org.example.supperapp.examservice.entity.Option;
import org.example.supperapp.examservice.entity.Question;
import org.example.supperapp.examservice.entity.QuestionGroup;
import org.example.supperapp.examservice.entity.enumeric.PartType;
import org.example.supperapp.examservice.exception.PdfImportException;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ToeicPdfParser {

    private static final Pattern PART_HEADING =
            Pattern.compile("(?im)^\\s*PART\\s+([1-7])\\b");
    private static final Pattern GROUP_HEADING = Pattern.compile(
            "(?im)^\\s*Questions?\\s+(\\d{1,3})\\s*[\\p{Pd}-]\\s*(\\d{1,3})"
                    + "\\s+refer(?:s)?\\s+to\\s+the\\s+following\\s+([^\\r\\n]+)");
    private static final Pattern ANY_QUESTION_START =
            Pattern.compile("(?m)^\\s*(\\d{1,3})\\s*[.]\\s*");
    private static final Pattern PAGE_NOISE = Pattern.compile(
            "(?im)^\\s*(?:GO\\s*ON\\s*TO\\s*THE\\s*NEXT\\s*PAGE|TEST\\s+\\d+\\s+\\d+|PART\\s+[1-7]|Directions:)\\b.*$");
    private static final Pattern GROUP_NOISE = Pattern.compile(
            "(?im)^\\s*Questions?\\s+\\d{1,3}\\s*[\\p{Pd}-]\\s*\\d{1,3}\\s+refer(?:s)?\\b.*$");
    private static final List<String> FOUR_LABELS = List.of("A", "B", "C", "D");
    private static final List<String> THREE_LABELS = List.of("A", "B", "C");
    private static final int OCR_DPI = 220;

    private final ITesseract tesseract;

    public List<Exam> parse(PDDocument document, int year, int testNumber) {
        if (document.getNumberOfPages() == 0) {
            throw new PdfImportException("PDF has no pages");
        }

        ParsingContext context = new ParsingContext(document);
        Map<Integer, Integer> partPages = findPartPages(context);
        boolean listening = looksLikeListening(partPages);
        boolean reading = looksLikeReading(partPages);

        if (!listening && !reading) {
            throw new PdfImportException(
                    "Unsupported PDF: expected an ETS TOEIC Listening or Reading test book");
        }

        List<Exam> parts = new ArrayList<>();
        if (listening) {
            requireParts(partPages, 1, 2, 3, 4);
            parts.add(parsePart1(context, partPages, year, testNumber));
            parts.add(parsePart2(year, testNumber));
            parts.add(parseListeningPart(
                    context, partPages, year, testNumber, 3, 32, 70, "Conversations"));
            parts.add(parseListeningPart(
                    context, partPages, year, testNumber, 4, 71, 100, "Talks"));
        }
        if (reading) {
            requireParts(partPages, 5, 6, 7);
            parts.add(parsePart5(context, partPages, year, testNumber));
            parts.add(parseReadingPart(
                    context, partPages, year, testNumber, 6, 131, 146, "Text Completion"));
            parts.add(parseReadingPart(
                    context, partPages, year, testNumber, 7, 147, 200, "Reading Comprehension"));
        }
        return parts;
    }

    private boolean looksLikeListening(Map<Integer, Integer> partPages) {
        return partPages.containsKey(1)
                && partPages.containsKey(2)
                && partPages.containsKey(3)
                && partPages.containsKey(4);
    }

    private boolean looksLikeReading(Map<Integer, Integer> partPages) {
        return partPages.containsKey(5) && partPages.containsKey(6) && partPages.containsKey(7);
    }

    private Map<Integer, Integer> findPartPages(ParsingContext context) {
        Map<Integer, Integer> result = new HashMap<>();
        for (int page = 0; page < context.pageCount(); page++) {
            Matcher matcher = PART_HEADING.matcher(context.naturalText(page));
            while (matcher.find()) {
                result.putIfAbsent(Integer.parseInt(matcher.group(1)), page);
            }
        }
        return result;
    }

    private void requireParts(Map<Integer, Integer> partPages, int... requiredParts) {
        for (int part : requiredParts) {
            if (!partPages.containsKey(part)) {
                throw new PdfImportException("Cannot find PART " + part + " heading in PDF");
            }
        }
    }

    private Exam parsePart1(
            ParsingContext context,
            Map<Integer, Integer> partPages,
            int year,
            int testNumber) {
        int firstPhotoPage = partPages.get(1) + 1;
        int part2Page = partPages.get(2);
        List<Question> questions = new ArrayList<>();

        for (int page = firstPhotoPage; page < part2Page && questions.size() < 6; page++) {
            BufferedImage rendered = context.render(page, 160);
            int split = rendered.getHeight() / 2;
            questions.add(imageQuestion(
                    questions.size() + 1,
                    rendered.getSubimage(0, 0, rendered.getWidth(), split)));
            if (questions.size() < 6) {
                questions.add(imageQuestion(
                        questions.size() + 1,
                        rendered.getSubimage(
                                0,
                                split,
                                rendered.getWidth(),
                                rendered.getHeight() - split)));
            }
        }

        assertQuestionCount(1, questions.size(), 6);
        return exam(year, testNumber, 1, PartType.LISTENING, "Photographs", questions, null);
    }

    private Question imageQuestion(int questionNumber, BufferedImage image) {
        return Question.builder()
                .questionNumber(questionNumber)
                .imageBase64(toJpegBase64(image, 750, 0.72f))
                .options(emptyOptions(FOUR_LABELS))
                .build();
    }

    private Exam parsePart2(int year, int testNumber) {
        List<Question> questions = new ArrayList<>();
        for (int number = 7; number <= 31; number++) {
            questions.add(Question.builder()
                    .questionNumber(number)
                    .options(emptyOptions(THREE_LABELS))
                    .build());
        }
        return exam(
                year,
                testNumber,
                2,
                PartType.LISTENING,
                "Question-Response",
                questions,
                null);
    }

    private Exam parseListeningPart(
            ParsingContext context,
            Map<Integer, Integer> partPages,
            int year,
            int testNumber,
            int partNumber,
            int firstQuestion,
            int lastQuestion,
            String title) {
        int startPage = partPages.get(partNumber);
        int endPage = partNumber == 4 ? context.pageCount() : partPages.get(partNumber + 1);
        String questionText = context.columnText(startPage, endPage);
        List<Question> questions = parseQuestions(questionText, firstQuestion, lastQuestion);
        attachGraphicPages(context, startPage, endPage, questions);

        List<QuestionGroup> groups = new ArrayList<>();
        int groupNumber = 1;
        for (int offset = 0; offset < questions.size(); offset += 3) {
            groups.add(QuestionGroup.builder()
                    .groupNumber(groupNumber++)
                    .transcript(null)
                    .questions(new ArrayList<>(questions.subList(offset, offset + 3)))
                    .build());
        }

        return exam(year, testNumber, partNumber, PartType.LISTENING, title, null, groups);
    }

    private void attachGraphicPages(
            ParsingContext context,
            int startPage,
            int endPage,
            List<Question> questions) {
        Map<Integer, String> renderedPages = new HashMap<>();
        questions.stream()
                .filter(question -> question.getContent() != null)
                .filter(question -> question.getContent().toLowerCase(Locale.ROOT).contains("graphic"))
                .forEach(question -> {
                    int page = findQuestionPage(
                            context,
                            startPage,
                            endPage,
                            question.getQuestionNumber());
                    String image = renderedPages.computeIfAbsent(
                            page,
                            key -> toJpegBase64(context.render(key, 140), 900, 0.72f));
                    question.setImageBase64(image);
                });
    }

    private int findQuestionPage(
            ParsingContext context, int startPage, int endPage, int questionNumber) {
        for (int page = startPage; page < endPage; page++) {
            if (parseQuestionByNumber(context.columnText(page, page + 1), questionNumber) != null) {
                return page;
            }
        }
        throw new PdfImportException(
                "Cannot locate source page for graphic question " + questionNumber);
    }

    private Exam parsePart5(
            ParsingContext context,
            Map<Integer, Integer> partPages,
            int year,
            int testNumber) {
        String text = context.columnText(partPages.get(5), partPages.get(6));
        List<Question> questions = parseQuestions(text, 101, 130);
        return exam(
                year,
                testNumber,
                5,
                PartType.READING,
                "Incomplete Sentences",
                questions,
                null);
    }

    private Exam parseReadingPart(
            ParsingContext context,
            Map<Integer, Integer> partPages,
            int year,
            int testNumber,
            int partNumber,
            int firstQuestion,
            int lastQuestion,
            String title) {
        int startPage = partPages.get(partNumber);
        int endPage = partNumber == 7 ? context.pageCount() : partPages.get(partNumber + 1);
        String questionText = context.columnText(startPage, endPage);
        String naturalText = context.naturalText(startPage, endPage);
        List<Question> questions = parseQuestions(questionText, firstQuestion, lastQuestion);
        if (partNumber == 6) {
            questions.forEach(question -> {
                if (question.getContent() == null || question.getContent().isBlank()) {
                    question.setContent("[" + question.getQuestionNumber() + "]");
                }
            });
        }

        List<QuestionGroup> groups = parseReadingGroups(
                naturalText, questions, partNumber, firstQuestion, lastQuestion);
        return exam(year, testNumber, partNumber, PartType.READING, title, null, groups);
    }

    List<Question> parseQuestions(String text, int firstQuestion, int lastQuestion) {
        List<Question> result = new ArrayList<>();
        for (int questionNumber = firstQuestion; questionNumber <= lastQuestion; questionNumber++) {
            Question question = parseQuestionByNumber(text, questionNumber);
            if (question == null) {
                throw new PdfImportException("Question "
                        + questionNumber
                        + " is missing or does not contain options A-D. "
                        + "The PDF layout may be unsupported or the text layer may be damaged.");
            }
            result.add(question);
        }
        return result;
    }

    Question parseQuestionByNumber(String text, int questionNumber) {
        Pattern target = Pattern.compile(
                "(?m)^\\s*" + questionNumber + "\\s*[.]\\s*");
        Matcher candidates = target.matcher(text);

        while (candidates.find()) {
            int blockStart = candidates.end();
            int blockEnd = findNextQuestionStart(text, blockStart);
            String block = text.substring(blockStart, blockEnd);
            ParsedOptions parsed = parseOptions(block);
            if (parsed == null) {
                continue;
            }

            String content = cleanInline(block.substring(0, parsed.a().start()));
            return Question.builder()
                    .questionNumber(questionNumber)
                    .content(content.isBlank() ? null : content)
                    .options(List.of(
                            option("A", slice(block, parsed.a().end(), parsed.b().start())),
                            option("B", slice(block, parsed.b().end(), parsed.c().start())),
                            option("C", slice(block, parsed.c().end(), parsed.d().start())),
                            option("D", cleanOptionD(block.substring(parsed.d().end())))))
                    .build();
        }
        return null;
    }

    private int findNextQuestionStart(String text, int fromIndex) {
        Matcher nextQuestion = ANY_QUESTION_START.matcher(text);
        return nextQuestion.find(fromIndex) ? nextQuestion.start() : text.length();
    }

    private ParsedOptions parseOptions(String block) {
        Marker a = findOption(block, "A", 0);
        Marker b = a == null ? null : findOption(block, "B", a.end());
        Marker c = b == null ? null : findOption(block, "C", b.end());
        Marker d = c == null ? null : findOption(block, "D", c.end());
        return d == null ? null : new ParsedOptions(a, b, c, d);
    }

    private Marker findOption(String text, String label, int fromIndex) {
        Pattern pattern = Pattern.compile(
                "(?m)^\\s*\\(\\s*" + Pattern.quote(label) + "\\s*\\)\\s*");
        Matcher matcher = pattern.matcher(text);
        return matcher.find(fromIndex) ? new Marker(matcher.start(), matcher.end()) : null;
    }

    private Option option(String label, String text) {
        String cleaned = cleanInline(text);
        return Option.builder()
                .label(label)
                .text(cleaned.isBlank() ? null : cleaned)
                // These test books do not include an answer key. null means "unknown".
                .correct(null)
                .build();
    }

    private String slice(String text, int start, int end) {
        return cleanInline(text.substring(start, end));
    }

    private String cleanOptionD(String text) {
        int end = text.length();
        Matcher pageNoise = PAGE_NOISE.matcher(text);
        if (pageNoise.find()) {
            end = Math.min(end, pageNoise.start());
        }
        Matcher groupNoise = GROUP_NOISE.matcher(text);
        if (groupNoise.find()) {
            end = Math.min(end, groupNoise.start());
        }
        return cleanInline(text.substring(0, end));
    }

    private List<QuestionGroup> parseReadingGroups(
            String naturalText,
            List<Question> questions,
            int partNumber,
            int firstQuestion,
            int lastQuestion) {
        List<GroupHeader> headers = findGroupHeaders(naturalText, firstQuestion, lastQuestion);
        validateGroupCoverage(headers, firstQuestion, lastQuestion);

        Map<Integer, Question> byNumber = new LinkedHashMap<>();
        questions.forEach(question -> byNumber.put(question.getQuestionNumber(), question));

        List<QuestionGroup> groups = new ArrayList<>();
        for (int index = 0; index < headers.size(); index++) {
            GroupHeader header = headers.get(index);
            int blockEnd = index + 1 < headers.size()
                    ? headers.get(index + 1).headerStart()
                    : naturalText.length();
            String block = naturalText.substring(header.contentStart(), blockEnd);
            int passageEnd = findPassageEnd(block, header.firstQuestion(), partNumber);
            String passage = cleanPassage(block.substring(0, passageEnd), partNumber);

            List<Question> groupQuestions = new ArrayList<>();
            for (int number = header.firstQuestion(); number <= header.lastQuestion(); number++) {
                Question question = byNumber.get(number);
                if (question == null) {
                    throw new PdfImportException("Question " + number + " is missing from its passage group");
                }
                groupQuestions.add(question);
            }

            groups.add(QuestionGroup.builder()
                    .groupNumber(groups.size() + 1)
                    .passage(passage.isBlank() ? null : passage)
                    .questions(groupQuestions)
                    .build());
        }
        return groups;
    }

    List<GroupHeader> findGroupHeaders(String text, int firstQuestion, int lastQuestion) {
        List<GroupHeader> headers = new ArrayList<>();
        Matcher matcher = GROUP_HEADING.matcher(text);
        while (matcher.find()) {
            int first = Integer.parseInt(matcher.group(1));
            int last = Integer.parseInt(matcher.group(2));
            if (first >= firstQuestion && last <= lastQuestion) {
                headers.add(new GroupHeader(first, last, matcher.start(), matcher.end()));
            }
        }
        return headers;
    }

    private void validateGroupCoverage(
            List<GroupHeader> headers, int firstQuestion, int lastQuestion) {
        int expected = firstQuestion;
        for (GroupHeader header : headers) {
            if (header.firstQuestion() != expected || header.lastQuestion() < header.firstQuestion()) {
                throw new PdfImportException(
                        "Passage groups are incomplete near question " + expected);
            }
            expected = header.lastQuestion() + 1;
        }
        if (expected != lastQuestion + 1) {
            throw new PdfImportException("Passage group is missing for questions "
                    + expected
                    + "-"
                    + lastQuestion);
        }
    }

    private int findPassageEnd(String block, int firstQuestion, int partNumber) {
        Pattern firstPrintedQuestion = partNumber == 6
                ? Pattern.compile("(?m)^\\s*" + firstQuestion + "\\s*[.]\\s*\\(\\s*A\\s*\\)")
                : Pattern.compile("(?m)^\\s*" + firstQuestion + "\\s*[.]\\s+\\S");
        Matcher matcher = firstPrintedQuestion.matcher(block);
        return matcher.find() ? matcher.start() : block.length();
    }

    private String cleanPassage(String text, int partNumber) {
        String cleaned = text.replace('\u00A0', ' ')
                .replace('\f', '\n')
                .replaceAll("(?im)^\\s*GO\\s*ON\\s*TO\\s*THE\\s*NEXT\\s*PAGE\\s*$", "")
                .replaceAll("(?im)^\\s*TEST\\s+\\d+\\s+\\d+\\s*$", "")
                .replaceAll("(?m)[ \\t]+$", "")
                .replaceAll("(?m)^[ \\t]+", "")
                .replaceAll("[ \\t]+", " ");
        if (partNumber == 6) {
            cleaned = cleaned.replaceAll("(?m)^\\s*(1(?:3[1-9]|4[0-6]))\\s*[.]\\s*$", "[$1]");
        }
        return cleaned.replaceAll("\\n{3,}", "\\n\\n").trim();
    }

    private String cleanInline(String text) {
        return text.replace('\u00A0', ' ')
                .replaceAll("(?im)^\\s*GO\\s*ON\\s*TO\\s*THE\\s*NEXT\\s*PAGE\\s*$", "")
                .replaceAll("(?im)^\\s*TEST\\s+\\d+\\s+\\d+\\s*$", "")
                .replaceAll("(?im)^\\s*This\\s+is\\s+the\\s+end\\s+of\\s+th\\s*$", "")
                .replaceAll("(?im)^\\s*e\\s+Listening\\s+test[.]?\\s*$", "")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\s*\\R\\s*", " ")
                .trim();
    }

    private List<Option> emptyOptions(List<String> labels) {
        return labels.stream()
                .map(label -> Option.builder().label(label).correct(null).build())
                .toList();
    }

    private Exam exam(
            int year,
            int testNumber,
            int partNumber,
            PartType type,
            String title,
            List<Question> questions,
            List<QuestionGroup> groups) {
        return Exam.builder()
                .year(year)
                .testNumber(testNumber)
                .partNumber(partNumber)
                .type(type)
                .title(title)
                .questions(questions)
                .groups(groups)
                .build();
    }

    private void assertQuestionCount(int partNumber, int actual, int expected) {
        if (actual != expected) {
            throw new PdfImportException("PART "
                    + partNumber
                    + " contains "
                    + actual
                    + " questions; expected "
                    + expected);
        }
    }

    private String toJpegBase64(BufferedImage source, int targetWidth, float quality) {
        int width = Math.min(targetWidth, source.getWidth());
        int height = Math.max(1, source.getHeight() * width / source.getWidth());
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam parameter = writer.getDefaultWriteParam();
            parameter.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parameter.setCompressionQuality(quality);
            writer.write(null, new IIOImage(resized, null, null), parameter);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException exception) {
            throw new PdfImportException("Cannot encode PART 1 photograph", exception);
        } finally {
            writer.dispose();
        }
    }

    record GroupHeader(
            int firstQuestion, int lastQuestion, int headerStart, int contentStart) {}

    private record Marker(int start, int end) {}

    private record ParsedOptions(Marker a, Marker b, Marker c, Marker d) {}

    private final class ParsingContext {
        private final PDDocument document;
        private final PDFRenderer renderer;
        private final Map<Integer, String> naturalText = new HashMap<>();
        private final Map<Integer, String> columnText = new HashMap<>();

        private ParsingContext(PDDocument document) {
            this.document = document;
            this.renderer = new PDFRenderer(document);
        }

        private int pageCount() {
            return document.getNumberOfPages();
        }

        private String naturalText(int pageIndex) {
            return naturalText.computeIfAbsent(pageIndex, this::extractNaturalText);
        }

        private String naturalText(int startPage, int endPageExclusive) {
            StringBuilder result = new StringBuilder();
            for (int page = startPage; page < endPageExclusive; page++) {
                result.append(naturalText(page)).append("\n\f\n");
            }
            return result.toString();
        }

        private String columnText(int startPage, int endPageExclusive) {
            StringBuilder result = new StringBuilder();
            for (int page = startPage; page < endPageExclusive; page++) {
                result.append(columnText.computeIfAbsent(page, this::extractColumnText))
                        .append("\n\f\n");
            }
            return result.toString();
        }

        private String extractNaturalText(int pageIndex) {
            try {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                stripper.setStartPage(pageIndex + 1);
                stripper.setEndPage(pageIndex + 1);
                String text = stripper.getText(document);
                return text.isBlank() ? ocr(render(pageIndex, OCR_DPI)) : text;
            } catch (IOException exception) {
                throw new PdfImportException(
                        "Cannot extract text from PDF page " + (pageIndex + 1), exception);
            }
        }

        private String extractColumnText(int pageIndex) {
            PDPage page = document.getPage(pageIndex);
            PDRectangle cropBox = page.getCropBox();
            try {
                PDFTextStripperByArea stripper = new PDFTextStripperByArea();
                stripper.setSortByPosition(true);
                float half = cropBox.getWidth() / 2f;
                // A few ETS question numbers touch the center gutter (notably question 100).
                // A small overlap prevents PDFBox from returning "00." in the right column.
                float overlap = Math.min(12f, cropBox.getWidth() * 0.02f);
                stripper.addRegion(
                        "left",
                        new Rectangle2D.Float(0, 0, half, cropBox.getHeight()));
                stripper.addRegion(
                        "right",
                        new Rectangle2D.Float(
                                half - overlap,
                                0,
                                half + overlap,
                                cropBox.getHeight()));
                stripper.extractRegions(page);
                String right = stripper.getTextForRegion("right")
                        // In some scans the leading "1" of 100 is placed just left of the
                        // center divider even when the rest of the question is in the right column.
                        .replaceAll("(?m)^\\s*00\\s*[.]", "100.");
                String text = stripper.getTextForRegion("left")
                        + "\n"
                        + right;
                if (!text.isBlank()) {
                    return text;
                }

                BufferedImage image = render(pageIndex, OCR_DPI);
                int halfImage = image.getWidth() / 2;
                int overlapImage = Math.min(
                        image.getWidth() / 50,
                        Math.max(1, image.getWidth() - halfImage - 1));
                return ocr(image.getSubimage(
                                0,
                                0,
                                halfImage,
                                image.getHeight()))
                        + "\n"
                        + ocr(image.getSubimage(
                                halfImage - overlapImage,
                                0,
                                image.getWidth() - halfImage + overlapImage,
                                image.getHeight()));
            } catch (IOException exception) {
                throw new PdfImportException(
                        "Cannot extract columns from PDF page " + (pageIndex + 1), exception);
            }
        }

        private BufferedImage render(int pageIndex, int dpi) {
            try {
                return renderer.renderImageWithDPI(pageIndex, dpi);
            } catch (IOException exception) {
                throw new PdfImportException(
                        "Cannot render PDF page " + (pageIndex + 1), exception);
            }
        }

        private String ocr(BufferedImage image) {
            try {
                synchronized (tesseract) {
                    return tesseract.doOCR(image);
                }
            } catch (TesseractException exception) {
                throw new PdfImportException(
                        "PDF has no usable text layer and OCR failed", exception);
            }
        }
    }
}
