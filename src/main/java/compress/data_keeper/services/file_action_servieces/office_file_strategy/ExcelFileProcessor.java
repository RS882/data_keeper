package compress.data_keeper.services.file_action_servieces.office_file_strategy;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class ExcelFileProcessor implements FileProcessorStrategy {
    @Override
    public Map<String, InputStream> process(InputStream inputStream) throws IOException {
        try (XSSFWorkbook excel = new XSSFWorkbook(inputStream)) {
            inputStream.reset();
            return Map.of();
        }
    }
}
