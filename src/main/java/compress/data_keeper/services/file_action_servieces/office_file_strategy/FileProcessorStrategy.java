package compress.data_keeper.services.file_action_servieces.office_file_strategy;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public interface FileProcessorStrategy {
    Map<String, InputStream> process(InputStream inputStream) throws IOException;
}
