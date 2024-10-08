package compress.data_keeper.services.file_action_servieces;

import compress.data_keeper.exception_handler.server_exception.ServerIOException;
import compress.data_keeper.services.file_action_servieces.interfaces.FileActionService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Collectors;

import static compress.data_keeper.constants.ImgConstants.IMAGE_SIZES;

@Service
public class TextFileActionServiceImpl implements FileActionService {

    @Override
    public Map<String, InputStream> getFileImages(MultipartFile file) {

        try {
            String content = new String(file.getBytes(), "UTF-8");

            BufferedImage image = convertTextToImage(content, 40);

            return IMAGE_SIZES.stream()
                    .collect(Collectors.toMap(
                            size -> size[0] + "x" + size[1],
                            size -> compressImg(image, size)
                    ));
        } catch (IOException e) {
            throw new ServerIOException(e.getMessage());
        }
    }
}
