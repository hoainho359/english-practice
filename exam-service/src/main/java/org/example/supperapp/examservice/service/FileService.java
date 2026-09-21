package org.example.supperapp.examservice.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.example.supperapp.examservice.entity.ExamEntity;
import org.example.supperapp.examservice.entity.OptionEntity;
import org.example.supperapp.examservice.entity.PassageEntity;
import org.example.supperapp.examservice.entity.QuestionEntity;
import org.example.supperapp.examservice.entity.enumeric.ContextType;
import org.example.supperapp.examservice.exception.PdfImportException;
import org.example.supperapp.examservice.repository.ExamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileService {

    private final ExamRepository examRepository;
    private final ToeicPdfParser toeicPdfParser;
    private final ListeningAnswerPdfParser listeningAnswerPdfParser;

    @Transactional
    public void uploadFile(MultipartFile file, Integer year, Integer testNumber) {
        validateRequest(file, year, testNumber);

        try (InputStream input = file.getInputStream();
                RandomAccessReadBuffer pdfBuffer = new RandomAccessReadBuffer(input);
                PDDocument document = Loader.loadPDF(pdfBuffer)) {
            if (document.isEncrypted()) {
                throw new PdfImportException("PDF is password-protected and cannot be imported");
            }

            List<ExamEntity> parsedParts = toeicPdfParser.parse(document, year, testNumber);

            // Do not persist anything until every expected question has been parsed and validated.
            examRepository.saveAll(parsedParts);
        } catch (PdfImportException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new PdfImportException("The uploaded file is not a readable PDF", exception);
        }
    }

    @Transactional
    public void uploadListeningAnswer(MultipartFile file, Integer year, Integer testNumber) {
        validateRequest(file, year, testNumber);

        try (InputStream input = file.getInputStream();
                RandomAccessReadBuffer pdfBuffer = new RandomAccessReadBuffer(input);
                PDDocument document = Loader.loadPDF(pdfBuffer)) {
            if (document.isEncrypted()) {
                throw new PdfImportException("Listening answer PDF is password-protected");
            }

            ListeningAnswerPdfParser.ListeningAnswerData answerData = listeningAnswerPdfParser.parse(document);
            List<ExamEntity> listeningParts =
                    examRepository.findAllByYearAndTestNumberOrderByPartNumber(year, testNumber).stream()
                            .filter(exam -> exam.getPartNumber() >= 1 && exam.getPartNumber() <= 4)
                            .toList();
            applyListeningAnswers(listeningParts, answerData, year, testNumber);
            examRepository.saveAll(listeningParts);
        } catch (PdfImportException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new PdfImportException("The uploaded listening answer file is not a readable PDF", exception);
        }
    }

    private void applyListeningAnswers(
            List<ExamEntity> parts, ListeningAnswerPdfParser.ListeningAnswerData answerData, int year, int testNumber) {
        if (parts.size() != 4) {
            throw new PdfImportException(
                    "Listening test " + year + "-" + testNumber + " must be imported before its transcript/answer PDF");
        }

        Map<Integer, ExamEntity> byPart =
                parts.stream().collect(Collectors.toMap(ExamEntity::getPartNumber, Function.identity()));
        Map<Integer, QuestionEntity> byQuestion = new LinkedHashMap<>();
        parts.forEach(part -> {
            part.getQuestions().forEach(question -> byQuestion.put(question.getQuestionNumber(), question));
            part.getPassages().forEach(passage -> passage.getQuestions()
                    .forEach(question -> byQuestion.put(question.getQuestionNumber(), question)));
        });
        if (byQuestion.size() != 100) {
            throw new PdfImportException(
                    "Imported listening test contains " + byQuestion.size() + " questions; expected 100");
        }

        answerData.answerKey().forEach((number, label) -> {
            QuestionEntity question = requireQuestion(byQuestion, number);
            if (question.getOptions().stream().noneMatch(option -> label.equals(option.getLabel()))) {
                throw new PdfImportException("Correct option " + label + " does not exist for question " + number);
            }
            question.setCorrectOption(label);
        });

        answerData.spokenQuestions().forEach((number, spoken) -> {
            QuestionEntity question = requireQuestion(byQuestion, number);
            updateOptionText(question, spoken.options());
            ExamEntity exam = byPart.get(number <= 6 ? 1 : 2);
            PassageEntity passage = findOrCreatePassage(exam, number, number, number);
            passage.setContextType(ContextType.LISTENING_TRANSCRIPT);
            passage.setContent(spoken.transcript());
            if (!passage.getQuestions().contains(question)) {
                passage.getQuestions().add(question);
            }
            question.setPassage(passage);
            question.setExam(exam);
        });

        answerData.groupTranscripts().forEach(group -> {
            int partNumber = group.firstQuestion() <= 70 ? 3 : 4;
            ExamEntity exam = byPart.get(partNumber);
            PassageEntity passage =
                    findOrCreatePassage(exam, group.firstQuestion(), group.firstQuestion(), group.lastQuestion());
            passage.setContextType(ContextType.LISTENING_TRANSCRIPT);
            passage.setContent(group.transcript());
        });

        parts.forEach(part -> {
            part.getPassages().sort(Comparator.comparing(PassageEntity::getFirstQuestionNumber));
            part.wireRelationships();
        });
    }

    private QuestionEntity requireQuestion(Map<Integer, QuestionEntity> questions, int number) {
        QuestionEntity question = questions.get(number);
        if (question == null) {
            throw new PdfImportException("Listening question " + number + " is missing from the database");
        }
        return question;
    }

    private void updateOptionText(QuestionEntity question, Map<String, String> optionTexts) {
        Map<String, OptionEntity> options =
                question.getOptions().stream().collect(Collectors.toMap(OptionEntity::getLabel, Function.identity()));
        optionTexts.forEach((label, text) -> {
            OptionEntity option = options.get(label);
            if (option == null) {
                throw new PdfImportException(
                        "Option " + label + " is missing for listening question " + question.getQuestionNumber());
            }
            option.setText(text);
        });
    }

    private PassageEntity findOrCreatePassage(ExamEntity exam, int groupNumber, int firstQuestion, int lastQuestion) {
        PassageEntity passage = exam.getPassages().stream()
                .filter(item ->
                        item.getFirstQuestionNumber() == firstQuestion && item.getLastQuestionNumber() == lastQuestion)
                .findFirst()
                .orElseGet(() -> {
                    PassageEntity created = PassageEntity.builder()
                            .groupNumber(groupNumber)
                            .partNumber(exam.getPartNumber())
                            .firstQuestionNumber(firstQuestion)
                            .lastQuestionNumber(lastQuestion)
                            .contextType(ContextType.LISTENING_TRANSCRIPT)
                            .questions(new ArrayList<>())
                            .build();
                    exam.addPassage(created);
                    return created;
                });
        passage.setExam(exam);
        return passage;
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
