package org.example.supperapp.examservice.service;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.example.supperapp.examservice.exception.PdfImportException;
import org.springframework.stereotype.Component;

@Component
public class ListeningAnswerPdfParser {

    private static final Pattern ANSWER_KEY =
            Pattern.compile("(?<!\\d)(\\d{1,3})\\s*\\(\\s*([ABCD0O])\\s*\\)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern SINGLE_QUESTION_HEADER =
            Pattern.compile("(?im)^\\s*(\\d{1,2})(?:[ \\t]+\\d{1,2})?\\s*(?:\\R\\s*)?"
                    + "((?:W|M)\\s*[-–]\\s*[A-Za-z]+"
                    + "(?:\\s*/\\s*(?:W|M)\\s*[-–]\\s*[A-Za-z]+)?)"
                    + "[^\\r\\n]*$");
    private static final Pattern GROUP_HEADER =
            Pattern.compile("(?im)^\\s*(\\d{1,3})\\s*[-–—]\\s*(\\d{2,3})\\b[^\\r\\n]*$");
    private static final Pattern OPTION_MARKER = Pattern.compile("(?m)^\\s*\\(\\s*([ABCD])\\s*\\)\\s*");

    public ListeningAnswerData parse(PDDocument document) {
        if (document.getNumberOfPages() == 0) {
            throw new PdfImportException("Listening answer PDF has no pages");
        }

        Map<Integer, String> answerKey = parseAnswerKey(extractAnswerKeyText(document));
        String columnText = extractColumnText(document);
        Map<Integer, SpokenQuestion> spokenQuestions = parseSpokenQuestions(columnText);
        List<GroupTranscript> groupTranscripts = parseGroupTranscripts(columnText);

        validate(answerKey, spokenQuestions, groupTranscripts);
        return new ListeningAnswerData(answerKey, spokenQuestions, groupTranscripts);
    }

    Map<Integer, String> parseAnswerKey(String text) {
        Map<Integer, String> answers = new LinkedHashMap<>();
        Matcher matcher = ANSWER_KEY.matcher(text);
        while (matcher.find()) {
            int questionNumber = Integer.parseInt(matcher.group(1));
            if (questionNumber < 1 || questionNumber > 100) {
                continue;
            }
            String rawLabel = matcher.group(2).toUpperCase();
            String label = rawLabel.equals("0") || rawLabel.equals("O") ? "C" : rawLabel;
            answers.putIfAbsent(questionNumber, label);
        }
        return answers;
    }

    Map<Integer, SpokenQuestion> parseSpokenQuestions(String text) {
        List<HeaderMatch> headers = new ArrayList<>();
        Matcher matcher = SINGLE_QUESTION_HEADER.matcher(text);
        while (matcher.find()) {
            int number = Integer.parseInt(matcher.group(1));
            if (number >= 1 && number <= 31) {
                headers.add(new HeaderMatch(number, matcher.start(), matcher.end()));
            }
        }

        Map<Integer, SpokenQuestion> questions = new LinkedHashMap<>();
        int expectedQuestionNumber = 1;
        for (int index = 0; index < headers.size(); index++) {
            HeaderMatch header = headers.get(index);
            int rawNumber = header.questionNumber();
            int questionNumber = rawNumber == expectedQuestionNumber
                            || expectedQuestionNumber >= 10 && rawNumber == expectedQuestionNumber % 10
                    ? expectedQuestionNumber
                    : rawNumber;
            if (questionNumber < 1 || questionNumber > 31) {
                continue;
            }
            expectedQuestionNumber = questionNumber + 1;
            int end = index + 1 < headers.size() ? headers.get(index + 1).start() : text.length();
            String block = text.substring(header.end(), end);
            int expectedOptions = questionNumber <= 6 ? 4 : 3;
            ParsedSpokenBlock parsed = parseSpokenBlock(block, expectedOptions);
            if (parsed != null) {
                questions.put(
                        questionNumber,
                        new SpokenQuestion(
                                questionNumber, questionNumber <= 6 ? null : parsed.prompt(), parsed.options()));
            }
        }
        return questions;
    }

    List<GroupTranscript> parseGroupTranscripts(String text) {
        List<GroupMatch> headers = new ArrayList<>();
        Matcher matcher = GROUP_HEADER.matcher(text);
        int expectedFirstQuestion = 32;
        while (matcher.find()) {
            int last = Integer.parseInt(matcher.group(2));
            int first = last - 2;
            if (first == expectedFirstQuestion && last <= 100) {
                headers.add(new GroupMatch(first, last, matcher.start(), matcher.end()));
                expectedFirstQuestion = last + 1;
            }
        }

        List<GroupTranscript> transcripts = new ArrayList<>();
        for (int index = 0; index < headers.size(); index++) {
            GroupMatch header = headers.get(index);
            int end = index + 1 < headers.size() ? headers.get(index + 1).start() : text.length();
            String block = text.substring(header.end(), end);
            String transcript = extractLeadingEnglishTranscript(block, header.firstQuestion(), header.lastQuestion());
            if (!transcript.isBlank()) {
                transcripts.add(new GroupTranscript(header.firstQuestion(), header.lastQuestion(), transcript));
            }
        }
        return transcripts;
    }

    private ParsedSpokenBlock parseSpokenBlock(String block, int expectedOptions) {
        List<Marker> markers = new ArrayList<>();
        Matcher matcher = OPTION_MARKER.matcher(block);
        while (matcher.find() && markers.size() < expectedOptions + 1) {
            markers.add(new Marker(matcher.group(1), matcher.start(), matcher.end()));
        }
        if (markers.size() < expectedOptions) {
            return null;
        }

        String prompt = cleanInline(block.substring(0, markers.getFirst().start()));
        Map<String, String> options = new LinkedHashMap<>();
        for (int index = 0; index < expectedOptions; index++) {
            Marker marker = markers.get(index);
            int end = index + 1 < markers.size() ? markers.get(index + 1).start() : block.length();
            String value = cleanInline(block.substring(marker.end(), end));
            if (value.isBlank()) {
                return null;
            }
            options.put(marker.label(), value);
        }
        return new ParsedSpokenBlock(prompt, options);
    }

    private String extractLeadingEnglishTranscript(String block, int firstQuestion, int lastQuestion) {
        List<String> accepted = new ArrayList<>();
        boolean started = false;
        for (String rawLine : block.replace('\u00A0', ' ').split("\\R")) {
            String line = rawLine.trim();
            if (line.isBlank()) {
                continue;
            }
            if (!started) {
                if (!line.matches("^(?:W|M)(?:\\s*[-–]\\s*[A-Za-z]+)?\\s+.*")) {
                    continue;
                }
                started = true;
            }
            if (!looksLikeEnglishTranscriptLine(line)) {
                break;
            }
            accepted.add(removeEmbeddedQuestionNumbers(line, firstQuestion, lastQuestion));
        }
        return String.join("\n", accepted).replaceAll("[ \\t]+", " ").trim();
    }

    private boolean looksLikeEnglishTranscriptLine(String line) {
        long letters = line.chars()
                .filter(character -> character >= 'A' && character <= 'Z' || character >= 'a' && character <= 'z')
                .count();
        if (letters < 2) {
            return false;
        }
        long suspicious = line.chars()
                .filter(character -> "#|^=[]<>".indexOf(character) >= 0)
                .count();
        return suspicious == 0;
    }

    private String removeEmbeddedQuestionNumbers(String line, int firstQuestion, int lastQuestion) {
        String cleaned = line;
        for (int number = firstQuestion; number <= lastQuestion; number++) {
            cleaned = cleaned.replaceAll("(?<!\\d)" + number + "(?=[A-Za-z])", "");
        }
        return cleaned;
    }

    private String cleanInline(String text) {
        String cleaned = text.replace('\u00A0', ' ')
                .replaceAll("(?im)^\\s*TEST\\s+\\d+\\s+\\d+\\s*$", "")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\s*\\R\\s*", " ")
                .trim();
        Matcher repeatedTranslation = Pattern.compile("\\s+\\(\\s*A\\s*\\)\\s+").matcher(cleaned);
        return repeatedTranslation.find()
                ? cleaned.substring(0, repeatedTranslation.start()).trim()
                : cleaned;
    }

    private void validate(
            Map<Integer, String> answerKey,
            Map<Integer, SpokenQuestion> spokenQuestions,
            List<GroupTranscript> groupTranscripts) {
        for (int number = 1; number <= 100; number++) {
            if (!answerKey.containsKey(number)) {
                throw new PdfImportException("Answer key is missing listening question " + number);
            }
        }
        for (int number = 1; number <= 31; number++) {
            if (!spokenQuestions.containsKey(number)) {
                throw new PdfImportException("Transcript is missing listening question " + number);
            }
        }

        int expected = 32;
        for (GroupTranscript transcript : groupTranscripts) {
            if (transcript.firstQuestion() != expected) {
                throw new PdfImportException("Transcript group is missing near question " + expected);
            }
            expected = transcript.lastQuestion() + 1;
        }
        if (expected != 101) {
            throw new PdfImportException("Transcript group is missing near question " + expected);
        }
    }

    private String extractAnswerKeyText(PDDocument document) {
        PDPage firstPage = document.getPage(0);
        PDRectangle cropBox = firstPage.getCropBox();
        try {
            PDFTextStripperByArea stripper = new PDFTextStripperByArea();
            stripper.setSortByPosition(true);
            stripper.addRegion(
                    "answer-key", new Rectangle2D.Float(0, 0, cropBox.getWidth() * 0.56f, cropBox.getHeight() * 0.52f));
            stripper.extractRegions(firstPage);
            return stripper.getTextForRegion("answer-key");
        } catch (IOException exception) {
            throw new PdfImportException("Cannot extract the listening answer key", exception);
        }
    }

    private String extractColumnText(PDDocument document) {
        StringBuilder result = new StringBuilder();
        for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
            PDPage page = document.getPage(pageIndex);
            PDRectangle cropBox = page.getCropBox();
            float width = cropBox.getWidth();
            float height = cropBox.getHeight();
            float gutter = Math.max(5f, width * 0.008f);
            float half = width / 2f;

            try {
                PDFTextStripperByArea stripper = new PDFTextStripperByArea();
                stripper.setSortByPosition(true);
                stripper.addRegion("left", new Rectangle2D.Float(0, 0, half + gutter, height));
                stripper.addRegion("right", new Rectangle2D.Float(half - gutter, 0, width - half + gutter, height));
                stripper.extractRegions(page);
                result.append(stripper.getTextForRegion("left"))
                        .append('\n')
                        .append(stripper.getTextForRegion("right"))
                        .append("\n\f\n");
            } catch (IOException exception) {
                throw new PdfImportException("Cannot extract listening answer PDF page " + (pageIndex + 1), exception);
            }
        }
        return result.toString();
    }

    public record ListeningAnswerData(
            Map<Integer, String> answerKey,
            Map<Integer, SpokenQuestion> spokenQuestions,
            List<GroupTranscript> groupTranscripts) {}

    public record SpokenQuestion(int questionNumber, String spokenPrompt, Map<String, String> options) {

        public String transcript() {
            StringBuilder result = new StringBuilder();
            if (spokenPrompt != null && !spokenPrompt.isBlank()) {
                result.append(spokenPrompt).append('\n');
            }
            options.forEach((label, value) ->
                    result.append('(').append(label).append(") ").append(value).append('\n'));
            return result.toString().trim();
        }
    }

    public record GroupTranscript(int firstQuestion, int lastQuestion, String transcript) {}

    private record HeaderMatch(int questionNumber, int start, int end) {}

    private record GroupMatch(int firstQuestion, int lastQuestion, int start, int end) {}

    private record Marker(String label, int start, int end) {}

    private record ParsedSpokenBlock(String prompt, Map<String, String> options) {}
}
