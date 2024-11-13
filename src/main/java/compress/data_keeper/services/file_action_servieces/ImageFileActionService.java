package compress.data_keeper.services.file_action_servieces;

import compress.data_keeper.exception_handler.server_exception.ServerIOException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Service
public class ImageFileActionService extends FileActionService {
    @Override
    public Map<String, InputStream> getFileImages(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            return getFileImagesByImg(image);
        } catch (IOException e) {
            throw new ServerIOException(e.getMessage());
        }
    }
}
