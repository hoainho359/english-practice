package org.example.supperapp.examservice.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;

@Configuration
@RequiredArgsConstructor
public class TesseractConfig {

    @Value("${tesseract.datapath}")
    private String dataPath;

    @Value("${tesseract.language:eng}")
    private String language;

    @Value("${tesseract.page-seg-mode:6}")
    private int pageSegMode;

    @Value("${tesseract.ocr-engine-mode:1}")
    private int ocrEngineMode;

    @Bean
    public ITesseract tesseract() {

        Tesseract tesseract = new Tesseract();

        tesseract.setDatapath(dataPath);
        tesseract.setLanguage(language);
        tesseract.setPageSegMode(pageSegMode);
        tesseract.setOcrEngineMode(ocrEngineMode);

        return tesseract;
    }
}
