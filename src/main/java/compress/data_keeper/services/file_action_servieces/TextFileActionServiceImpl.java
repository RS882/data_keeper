package compress.data_keeper.services.file_action_servieces;

import compress.data_keeper.exception_handler.server_exception.ServerIOException;
import compress.data_keeper.services.file_action_servieces.interfaces.FileActionService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static compress.data_keeper.constants.ImgConstants.IMAGE_SIZES;
import static compress.data_keeper.constants.MediaFormats.IMAGE_FORMAT;

@Service
public class TextFileActionServiceImpl implements FileActionService {

    @Override
    public Map<String, InputStream> getFileImages(MultipartFile file) {

//        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            String content = new String(file.getBytes(), "UTF-8");

            BufferedImage image = convertTextToImage(content, 40);

//            ImageIO.write(image, IMAGE_FORMAT, baos);
//        InputStream imgInputStream = new ByteArrayInputStream(baos.toByteArray());

            return IMAGE_SIZES.stream()
                    .collect(Collectors.toMap(
                            size -> size[0] + "x" + size[1],
                            size-> compressImg(image,size)
                    ));


        } catch (IOException e) {
            throw new ServerIOException(e.getMessage());
        }

    }

    private BufferedImage convertTextToImage(String text, int linesPerPage) {
        int width = 800;
        int height = 1000;

        String[] lines = text.split("\n");

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.BLACK);

        int y = 50;
        for (int i = 0; i < linesPerPage && i < lines.length; i++) {
            g.drawString(lines[i], 20, y);
            y += g.getFontMetrics().getHeight();
        }
        g.dispose();

        return image;
    }
}
