package compress.data_keeper.services.file_action_servieces;

import compress.data_keeper.exception_handler.server_exception.ServerIOException;
import compress.data_keeper.services.file_action_servieces.checked_function.CheckedFunction;
import compress.data_keeper.services.file_action_servieces.interfaces.FileActionService;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import static compress.data_keeper.services.file_action_servieces.checked_function.WrapCheckedFunction.wrapCheckedFunction;

@Service
@Slf4j
public class OOXMLFileActionServiceImp implements FileActionService {
    @Override
    public Map<String, InputStream> getFileImages(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            inputStream.mark(Integer.MAX_VALUE);
            Map<String, FileProcessorStrategy> fileProcessors = Map.of(
                    "excel", new ExcelFileProcessor(),
                    "powerpoint", new PowerPointFileProcessor(),
                    "word", new WordFileProcessor()
            );
            String fileType = getFileType(inputStream);
            FileProcessorStrategy processor = fileProcessors.get(fileType);
            return processor != null ? processor.process(inputStream) : Map.of();
        } catch (IOException e) {
            throw new ServerIOException("Error processing file");
        }
    }

    private String getFileType(InputStream inputStream) throws IOException {

        if (isFileType(inputStream, XSSFWorkbook::new)) {
            return "excel";
        }
        if (isFileType(inputStream, XMLSlideShow::new)) {
            return "powerpoint";
        }
        if (isFileType(inputStream, XWPFDocument::new)) {
            return "word";
        }
        return "unknown file type";
    }

    private boolean isFileType(InputStream inputStream, CheckedFunction<InputStream, ?> fileCreator) {
        try {
            wrapCheckedFunction(fileCreator).apply(inputStream);
            inputStream.reset();
            return true;
        } catch (Exception e) {
            try {
                inputStream.reset();
            } catch (IOException resetException) {
                log.error("Failed to reset InputStream after failed file type check", resetException);
            }
            return false;
        }
    }

    private void convertDocxToPdf(XWPFDocument docx, OutputStream outputStream) throws IOException {

        try (PDDocument pdfDocument = new PDDocument()) {

            PDPage page = new PDPage();
            pdfDocument.addPage(page);
            PDType0Font font = PDType0Font.load(pdfDocument, new File("src/main/resources/fonts/arial/arial.ttf"));
            PDPageContentStream contentStream = new PDPageContentStream(pdfDocument, page);
            contentStream.setFont(font, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(100, 700);

            for (XWPFParagraph paragraph : docx.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.isEmpty()) {
                    contentStream.showText(text);
                    contentStream.newLineAtOffset(0, -15);
                }
            }
            contentStream.endText();
            contentStream.close();
            pdfDocument.save(outputStream);
        }
    }

    private String convertDocxToTxt(XWPFDocument docx) {
        StringBuilder text = new StringBuilder();
        docx.getParagraphs().forEach(p -> text.append(p.getText()).append("\n"));
        return text.toString();
    }

    interface FileProcessorStrategy {
        Map<String, InputStream> process(InputStream inputStream);
    }

    class ExcelFileProcessor implements FileProcessorStrategy {
        @Override
        public Map<String, InputStream> process(InputStream inputStream) {
            try (XSSFWorkbook excel = new XSSFWorkbook(inputStream)) {
                return Map.of();
            } catch (IOException e) {
                throw new ServerIOException(e.getMessage());
            }
        }
    }

    class PowerPointFileProcessor implements FileProcessorStrategy {
        @Override
        public Map<String, InputStream> process(InputStream inputStream) {
            try (XMLSlideShow ppt = new XMLSlideShow(inputStream)) {
                return Map.of();
            } catch (IOException e) {
                throw new ServerIOException(e.getMessage());
            }
        }
    }

    class WordFileProcessor implements FileProcessorStrategy {
        @Override
        public Map<String, InputStream> process(InputStream inputStream) {
            try (XWPFDocument docx = new XWPFDocument(inputStream)) {
                String content = convertDocxToTxt(docx);
                return getFileImagesByTxt(content);
            } catch (IOException e) {
                throw new ServerIOException(e.getMessage());
            }
        }
    }
}
