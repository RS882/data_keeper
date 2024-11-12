package compress.data_keeper.services.file_action_servieces.office_file_strategy;


import org.apache.poi.xslf.usermodel.XMLSlideShow;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class PowerPointFileProcessor implements FileProcessorStrategy {
    @Override
    public Map<String, InputStream> process(InputStream inputStream) throws IOException {
        try (XMLSlideShow ppt = new XMLSlideShow(inputStream)) {
            inputStream.reset();
            return Map.of();
        }
    }
}
