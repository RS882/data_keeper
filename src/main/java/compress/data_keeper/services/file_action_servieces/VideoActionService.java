package compress.data_keeper.services.file_action_servieces;

import compress.data_keeper.exception_handler.server_exception.ServerIOException;
import compress.data_keeper.exception_handler.server_exception.exceptions.UploadException;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class VideoActionService extends FileActionService {
    @Override
    public Map<String, InputStream> getFileImages(MultipartFile file) {
        try (InputStream videoInputStream = file.getInputStream()) {
            BufferedImage image = getPreviewPictures(videoInputStream, file.getOriginalFilename());
            return getFileImagesByImg(image);
        } catch (IOException e) {
            throw new ServerIOException(e.getMessage());
        }
    }

    private BufferedImage getPreviewPictures(InputStream videoInputStream, String originalFilename) {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoInputStream)) {
            BufferedImage image = null;
            try (Java2DFrameConverter converter = new Java2DFrameConverter()) {
                grabber.start();
                for (int i = 0; i < 50; i++) {
                    image = converter.convert(grabber.grabKeyFrame());
                    if (image != null) {
                        break;
                    }
                }
            }
            grabber.stop();
            return image;
        } catch (Exception e) {
            throw new UploadException(
                    String.format("The preview picture for file  %s cannot be saved", originalFilename));
        }
    }
}
