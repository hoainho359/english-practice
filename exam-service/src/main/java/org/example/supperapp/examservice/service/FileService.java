package org.example.supperapp.examservice.service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.example.supperapp.examservice.entity.Exam;
import org.example.supperapp.examservice.entity.Option;
import org.example.supperapp.examservice.entity.Question;
import org.example.supperapp.examservice.entity.QuestionGroup;
import org.example.supperapp.examservice.entity.enumeric.PartType;
import org.example.supperapp.examservice.repository.ExamRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.ITesseract;

@Service
@RequiredArgsConstructor
public class FileService {

    private final ExamRepository examRepository;
    private final ITesseract tesseract;

    public void uploadFile(MultipartFile file, Integer year, Integer testNumber) throws Exception {

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {

            // Part 1 & 2 (Listening)
            examRepository.save(parsePart1(document, year, testNumber));
            examRepository.save(parsePart2(year, testNumber));

            // Part 3 & 4 (Listening)
            examRepository.save(parsePart3(document, year, testNumber));
            examRepository.save(parsePart4(document, year, testNumber));

            // Part 5, 6, 7 (Reading)
            examRepository.save(parsePart5(document, year, testNumber));
            examRepository.save(parsePart6(document, year, testNumber));
            examRepository.save(parsePart7(document, year, testNumber));
        }
    }
    /* ==========================================================
    					PART 1
    ========================================================== */

    private Exam parsePart1(PDDocument document, Integer year, Integer testNumber) throws IOException {

        PDFRenderer renderer = new PDFRenderer(document);

        List<Question> questions = new ArrayList<>();

        int number = 1;

        for (int page = 1; page <= 3; page++) {

            BufferedImage img = renderer.renderImageWithDPI(page, 180);

            int w = img.getWidth();
            int h = img.getHeight();

            questions.add(buildImageQuestion(number++, img.getSubimage(0, 0, w, h / 2)));

            questions.add(buildImageQuestion(number++, img.getSubimage(0, h / 2, w, h / 2)));
        }

        return Exam.builder()
                .year(year)
                .testNumber(testNumber)
                .partNumber(1)
                .type(PartType.LISTENING)
                .title("Photographs")
                .questions(questions)
                .build();
    }

    private Question buildImageQuestion(int number, BufferedImage image) throws IOException {

        return Question.builder()
                .questionNumber(number)
                .imageBase64(toCompressedBase64(image))
                .options(new ArrayList<>())
                .build();
    }

    private String toCompressedBase64(BufferedImage image) throws IOException {

        BufferedImage resized = resize(image, 600);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();

        ImageOutputStream ios = ImageIO.createImageOutputStream(out);

        writer.setOutput(ios);

        ImageWriteParam param = writer.getDefaultWriteParam();

        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.6f);

        writer.write(null, new IIOImage(resized, null, null), param);

        ios.close();
        writer.dispose();

        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private BufferedImage resize(BufferedImage original, int width) {

        int height = original.getHeight() * width / original.getWidth();

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = img.createGraphics();

        g.drawImage(original, 0, 0, width, height, null);

        g.dispose();

        return img;
    }

    /* ==========================================================
    					PART 2
    ========================================================== */

    private Exam parsePart2(Integer year, Integer testNumber) {

        List<Question> questions = new ArrayList<>();

        for (int i = 7; i <= 31; i++) {

            questions.add(Question.builder()
                    .questionNumber(i)
                    .options(new ArrayList<>())
                    .build());
        }

        return Exam.builder()
                .year(year)
                .testNumber(testNumber)
                .partNumber(2)
                .type(PartType.LISTENING)
                .title("Question Response")
                .questions(questions)
                .build();
    }

    /* ==========================================================
    					PART 3 & PART 4 PARSER
    ========================================================== */

    private Exam parsePart3(PDDocument document, Integer year, Integer testNumber) throws Exception {

        int part3Page = findPartPage(document, "PART 3");
        int part4Page = findPartPage(document, "PART 4");

        return Exam.builder()
                .year(year)
                .testNumber(testNumber)
                .partNumber(3)
                .type(PartType.LISTENING)
                .title("Conversations")
                .groups(parseListening(document, part3Page, part4Page - 1, 32, 70))
                .build();
    }

    private Exam parsePart4(PDDocument document, Integer year, Integer testNumber) throws Exception {

        int part4Page = findPartPage(document, "PART 4");

        return Exam.builder()
                .year(year)
                .testNumber(testNumber)
                .partNumber(4)
                .type(PartType.LISTENING)
                .title("Talks")
                .groups(parseListening(document, part4Page, document.getNumberOfPages() - 1, 71, 100))
                .build();
    }

    private List<QuestionGroup> parseListening(PDDocument document, int startPage, int endPage, int startQ, int maxQ)
            throws IOException {

        List<QuestionGroup> groups = new ArrayList<>();

        // 1. Trích xuất văn bản theo TỪNG CỘT (trái xong mới đến phải)
        String fullText = extractTextWithColumns(document, startPage, endPage);

        // 2. Gom nhóm 3 câu hỏi liên tiếp (32-34, 35-37, v.v...)
        int groupNum = 1;
        for (int qNum = startQ; qNum <= maxQ; qNum += 3) {
            List<Question> qList = new ArrayList<>();

            for (int i = 0; i < 3; i++) {
                int targetQ = qNum + i;
                if (targetQ > maxQ) break;

                Question q = parseQuestionByNumber(fullText, targetQ);
                if (q != null) {
                    qList.add(q);
                }
            }

            if (!qList.isEmpty()) {
                groups.add(QuestionGroup.builder()
                        .groupNumber(groupNum++)
                        .questions(qList)
                        .build());
            }
        }

        return groups;
    }
    // HÀM MỚI: Xử lý đọc PDF chia 2 cột
    private String extractTextWithColumns(PDDocument document, int startPage, int endPage) throws IOException {
        StringBuilder fullTextBuilder = new StringBuilder();
        endPage = Math.min(endPage, document.getNumberOfPages() - 1);

        for (int page = startPage; page <= endPage; page++) {
            // Lấy object PDPage thay vì chỉ lấy text
            org.apache.pdfbox.pdmodel.PDPage pdPage = document.getPage(page);

            // Lấy kích thước thực tế của trang
            float width = pdPage.getMediaBox().getWidth();
            float height = pdPage.getMediaBox().getHeight();

            PDFTextStripperByArea stripper = new PDFTextStripperByArea();
            stripper.setSortByPosition(true);

            // Cắt trang làm 2 nửa: Cột trái và Cột phải
            java.awt.geom.Rectangle2D leftRect = new java.awt.geom.Rectangle2D.Float(0, 0, width / 2, height);
            java.awt.geom.Rectangle2D rightRect = new java.awt.geom.Rectangle2D.Float(width / 2, 0, width / 2, height);

            stripper.addRegion("leftColumn", leftRect);
            stripper.addRegion("rightColumn", rightRect);

            // Tiến hành extract
            stripper.extractRegions(pdPage);

            // Nối text cột trái trước, sau đó mới nối text cột phải
            fullTextBuilder.append(stripper.getTextForRegion("leftColumn")).append("\n");
            fullTextBuilder.append(stripper.getTextForRegion("rightColumn")).append("\n");
        }

        return fullTextBuilder.toString();
    }

    private Question parseQuestionByNumber(String fullText, int qNum) {
        // 1. Tìm điểm bắt đầu câu hỏi (vd: "32.")
        Pattern startPattern = Pattern.compile("(?:^|\\n|\\s)" + qNum + "\\.\\s*");
        Matcher startMatcher = startPattern.matcher(fullText);
        if (!startMatcher.find()) {
            return null;
        }
        int blockStart = startMatcher.end();

        // 2. Tìm điểm ngắt câu hỏi tại số câu tiếp theo (độ dài 2-3 chữ số)
        Pattern nextQPattern = Pattern.compile("(?:^|\\n|\\s)\\d{2,3}\\.\\s*");
        Matcher nextMatcher = nextQPattern.matcher(fullText);

        int blockEnd = fullText.length();
        if (nextMatcher.find(blockStart)) {
            blockEnd = nextMatcher.start();
        }
        String block = fullText.substring(blockStart, blockEnd);

        // 3. TÌM KIẾM TUẦN TỰ ĐỂ TRÁNH LỖI OutOfBounds
        int[] rangeA = findOptionRange(block, "A", 0);
        if (rangeA == null) return null;

        int[] rangeB = findOptionRange(block, "B", rangeA[1]);
        if (rangeB == null) return null;

        int[] rangeC = findOptionRange(block, "C", rangeB[1]);
        if (rangeC == null) return null;

        int[] rangeD = findOptionRange(block, "D", rangeC[1]);
        if (rangeD == null) return null;

        // 4. Cắt chuỗi an toàn vì rangeA[1] <= rangeB[0] <= rangeB[1]...
        String content = clean(block.substring(0, rangeA[0]));
        String textA = clean(block.substring(rangeA[1], rangeB[0]));
        String textB = clean(block.substring(rangeB[1], rangeC[0]));
        String textC = clean(block.substring(rangeC[1], rangeD[0]));
        String textD = clean(block.substring(rangeD[1]));

        return Question.builder()
                .questionNumber(qNum)
                .content(content)
                .options(List.of(
                        option("A", textA, false),
                        option("B", textB, false),
                        option("C", textC, false),
                        option("D", textD, false)))
                .explanation(null)
                .imageBase64(null)
                .build();
    }

    // Hàm tìm kiếm trả về mảng 2 phần tử: [vị_trí_bắt_đầu, vị_trí_kết_thúc]
    private int[] findOptionRange(String text, String label, int startIndex) {
        Pattern p = Pattern.compile("\\(\\s*" + label + "\\s*\\)");
        Matcher m = p.matcher(text);
        if (m.find(startIndex)) {
            return new int[] {m.start(), m.end()};
        }
        return null;
    }

    private int findPartPage(PDDocument document, String keyword) throws IOException {

        PDFTextStripper stripper = new PDFTextStripper();

        for (int page = 1; page <= document.getNumberOfPages(); page++) {

            stripper.setStartPage(page);
            stripper.setEndPage(page);

            String text = stripper.getText(document);

            if (text.contains(keyword)) {
                return page - 1;
            }
        }

        throw new IllegalStateException("Không tìm thấy " + keyword + " trong PDF");
    }

    private Option option(String label, String text, boolean correct) {
        return Option.builder().label(label).text(clean(text)).correct(correct).build();
    }

    private String clean(String text) {
        return text.replace('\u00A0', ' ') // non-breaking space
                .replaceAll("[ \t]+", " ")
                .replaceAll("\\s*\\n\\s*", " ")
                .trim();
    }
    /* ==========================================================
    					PART 5 PARSER
    ========================================================== */

    private Exam parsePart5(PDDocument document, Integer year, Integer testNumber) throws Exception {

        int part5Page = findPartPage(document, "PART 5");
        int part6Page = findPartPage(document, "PART 6");

        String fullText = extractTextWithColumns(document, part5Page, part6Page - 1);
        List<Question> questions = new ArrayList<>();

        for (int qNum = 101; qNum <= 130; qNum++) {
            Question q = parseQuestionByNumber(fullText, qNum);
            if (q != null) {
                questions.add(q);
            }
        }

        return Exam.builder()
                .year(year)
                .testNumber(testNumber)
                .partNumber(5)
                .type(PartType.READING)
                .title("Incomplete Sentences")
                .questions(questions)
                .build();
    }
    /* ==========================================================
    					PART 6 & PART 7 PARSER
    ========================================================== */

    private Exam parsePart6(PDDocument document, Integer year, Integer testNumber) throws Exception {

        int part6Page = findPartPage(document, "PART 6");
        int part7Page = findPartPage(document, "PART 7");

        List<QuestionGroup> groups = parseReadingPassageGroups(document, part6Page, part7Page - 1, 131, 146);

        return Exam.builder()
                .year(year)
                .testNumber(testNumber)
                .partNumber(6)
                .type(PartType.READING)
                .title("Text Completion")
                .groups(groups)
                .build();
    }

    private Exam parsePart7(PDDocument document, Integer year, Integer testNumber) throws Exception {

        int part7Page = findPartPage(document, "PART 7");
        int lastPage = document.getNumberOfPages() - 1;

        List<QuestionGroup> groups = parseReadingPassageGroups(document, part7Page, lastPage, 147, 200);

        return Exam.builder()
                .year(year)
                .testNumber(testNumber)
                .partNumber(7)
                .type(PartType.READING)
                .title("Reading Comprehension")
                .groups(groups)
                .build();
    }

    /**
     * Tách đoạn văn (Passage) và các câu hỏi thuộc Part 6 & Part 7
     */
    private List<QuestionGroup> parseReadingPassageGroups(
            PDDocument document, int startPage, int endPage, int minQ, int maxQ) throws IOException {

        List<QuestionGroup> groups = new ArrayList<>();
        String fullText = extractTextWithColumns(document, startPage, endPage);

        // Regex tìm tiêu đề nhóm: "Questions 131-134 refer to the following..."
        Pattern headerPattern = Pattern.compile(
                "Questions\\s+(\\d{3})\\s*-\\s*(\\d{3})\\s+refer\\s+to\\s+the\\s+following\\s+([^.\\n]+)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = headerPattern.matcher(fullText);

        List<int[]> groupBounds = new ArrayList<>();
        List<String> passageTitles = new ArrayList<>();
        List<Integer> startQs = new ArrayList<>();
        List<Integer> endQs = new ArrayList<>();

        while (matcher.find()) {
            int qStart = Integer.parseInt(matcher.group(1));
            int qEnd = Integer.parseInt(matcher.group(2));

            if (qStart >= minQ && qEnd <= maxQ) {
                groupBounds.add(new int[] {matcher.start(), matcher.end()});
                startQs.add(qStart);
                endQs.add(qEnd);
                passageTitles.add(matcher.group(3).trim());
            }
        }

        int groupNum = 1;
        for (int i = 0; i < groupBounds.size(); i++) {
            int blockStart = groupBounds.get(i)[1];
            int blockEnd = (i + 1 < groupBounds.size()) ? groupBounds.get(i + 1)[0] : fullText.length();

            String groupBlock = fullText.substring(blockStart, blockEnd);

            int startQ = startQs.get(i);
            int endQ = endQs.get(i);

            // Tìm vị trí bắt đầu của câu hỏi đầu tiên trong nhóm (vd: "131.")
            Pattern firstQPattern = Pattern.compile("(?:^|\\n|\\s)" + startQ + "\\.\\s*");
            Matcher firstQMatcher = firstQPattern.matcher(groupBlock);

            String passageText = "";
            if (firstQMatcher.find()) {
                passageText = cleanPassage(groupBlock.substring(0, firstQMatcher.start()));
            } else {
                passageText = cleanPassage(groupBlock);
            }

            // Parse danh sách câu hỏi trong nhóm
            List<Question> questions = new ArrayList<>();
            for (int qNum = startQ; qNum <= endQ; qNum++) {
                Question q = parseQuestionByNumber(groupBlock, qNum);
                if (q != null) {
                    questions.add(q);
                }
            }

            if (!questions.isEmpty()) {
                groups.add(QuestionGroup.builder()
                        .groupNumber(groupNum++)
                        .passage(passageText) // Đảm bảo Entity QuestionGroup có trường passage
                        .questions(questions)
                        .build());
            }
        }

        return groups;
    }

    private String cleanPassage(String text) {
        return text.replaceAll("GO ON TO THE NEXT PAGE", "")
                .replaceAll("TEST \\d+", "")
                .trim();
    }
}
