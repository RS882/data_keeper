package compress.data_keeper.services.file_action_servieces;

import compress.data_keeper.exception_handler.server_exception.ServerIOException;
import compress.data_keeper.services.file_action_servieces.checked_function.CheckedFunction;
import compress.data_keeper.services.file_action_servieces.office_file_strategy.ExcelFileProcessor;
import compress.data_keeper.services.file_action_servieces.office_file_strategy.FileProcessorStrategy;
import compress.data_keeper.services.file_action_servieces.office_file_strategy.PowerPointFileProcessor;
import compress.data_keeper.services.file_action_servieces.office_file_strategy.WordFileProcessor;
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
public class OOXMLFileActionService extends FileActionService {

    @Override
    public Map<String, InputStream> getFileImages(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            inputStream.mark(Integer.MAX_VALUE);

            FileProcessorStrategy processor = getProcessor(inputStream);
            return processor != null ? processor.process(inputStream) : Map.of();
        } catch (IOException e) {
            throw new ServerIOException("Error processing file");
        }
    }

    private FileProcessorStrategy getProcessor(InputStream inputStream) throws IOException {

        if (isFileType(inputStream, XSSFWorkbook::new)) {
            return new ExcelFileProcessor();
        }
        if (isFileType(inputStream, XMLSlideShow::new)) {
            return new PowerPointFileProcessor();
        }
        if (isFileType(inputStream, XWPFDocument::new)) {
            return new WordFileProcessor();
        }
        return null;
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
}
