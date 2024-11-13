package compress.data_keeper.services.file_action_servieces;

import compress.data_keeper.exception_handler.server_exception.ServerIOException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;

@Service
public class TextFileActionService extends FileActionService {

    @Override
    public Map<String, InputStream> getFileImages(MultipartFile file) {
        try {
            String content = new String(file.getBytes(), "UTF-8");
            return getFileImagesByTxt(content);
        } catch (Exception e) {
            throw new ServerIOException(e.getMessage());
        }
    }
}
