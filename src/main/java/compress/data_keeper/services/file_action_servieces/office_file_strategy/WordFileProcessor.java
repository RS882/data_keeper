package compress.data_keeper.services.file_action_servieces.office_file_strategy;


import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static compress.data_keeper.services.file_action_servieces.FileActionService.getFileImagesByTxt;

public class WordFileProcessor implements FileProcessorStrategy {

    @Override
    public Map<String, InputStream> process(InputStream inputStream) throws IOException {
        try (XWPFDocument docx = new XWPFDocument(inputStream)) {
            String content = convertDocxToTxt(docx);
            inputStream.reset();
            return getFileImagesByTxt(content);
        }
    }

    private String convertDocxToTxt(XWPFDocument docx) {
        StringBuilder text = new StringBuilder();
        docx.getParagraphs().forEach(p -> text.append(p.getText()).append("\n"));
        return text.toString();
    }
}
