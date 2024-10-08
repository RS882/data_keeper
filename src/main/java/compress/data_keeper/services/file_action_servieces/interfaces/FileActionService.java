package compress.data_keeper.services.file_action_servieces.interfaces;

import compress.data_keeper.exception_handler.server_exception.ServerIOException;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static compress.data_keeper.constants.MediaFormats.IMAGE_FORMAT;

public interface FileActionService {

    Map<String, InputStream> getFileImages(MultipartFile file);

    default
    BufferedImage convertTextToImage(String text, int linesPerPage) {
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

    default InputStream compressImg(BufferedImage image, int[] size) {

        ByteArrayOutputStream outputStream;
        try {

            int originalWidth = image.getWidth();
            int originalHeight = image.getHeight();

            double aspectRatio = (double) originalWidth / originalHeight;
            int newWidth, newHeight;

            if (originalWidth > originalHeight) {
                newWidth = size[0];
                newHeight = (int) (size[0] / aspectRatio);
            } else {
                newHeight = size[1];
                newWidth = (int) (size[1] * aspectRatio);
            }

            BufferedImage resizedImage = new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resizedImage.createGraphics();

            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, size[0], size[1]);

            int x = (size[0] - newWidth) / 2;
            int y = (size[1] - newHeight) / 2;
            g2d.drawImage(image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), x, y, null);
            g2d.dispose();

            outputStream = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, IMAGE_FORMAT, outputStream);
        } catch (IOException e) {
            throw new ServerIOException(e.getMessage());
        }
        return new ByteArrayInputStream(outputStream.toByteArray());
    }
}
