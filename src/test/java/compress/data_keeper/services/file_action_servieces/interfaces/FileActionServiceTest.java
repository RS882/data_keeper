package compress.data_keeper.services.file_action_servieces.interfaces;

import compress.data_keeper.exception_handler.bad_requeat.exceptions.TextIsNullException;
import compress.data_keeper.exception_handler.server_exception.ServerIOException;
import compress.data_keeper.services.file_action_servieces.TextFileActionServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(value = DisplayNameGenerator.ReplaceUnderscores.class)
class FileActionServiceTest {

    @InjectMocks
    private TextFileActionServiceImpl service;

    @Nested
    @DisplayName("Convert text to image test")
    class ConvertImageTest {

        @ParameterizedTest(name = "Тест {index}: convert image to text [{arguments}]")
        @MethodSource("textParams")
        public void positive_convert_image_to_text_test_with_all_param(String text, int linesPerPage) {

            BufferedImage result = service.convertTextToImage(text, linesPerPage);

            assertNotNull(result, "Image should not be null");
            assertEquals(800, result.getWidth(), "Image width should be 800");
            assertEquals(1000, result.getHeight(), "Image height should be 1000");

            int backgroundColor = Color.WHITE.getRGB();

            assertEquals(backgroundColor, result.getRGB(0, 0), "Top left corner should be white");
        }

        private static Stream<Arguments> textParams() {
            return Stream.of(
                    Arguments.of("Line 1\nLine 2\nLine 3\nLine 4\nLine 5", 3),
                    Arguments.of("", 3),
                    Arguments.of("Line 1\nLine 2\nLine 3\nLine 4\nLine 5", 0),
                    Arguments.of("Line 1\nLine 2\nLine 3\nLine 4\nLine 5", 1010101100),
                    Arguments.of("Line 1\nLine 2\nLine 3\nLine 4\nLine 5", -1010101100),
                    Arguments.of("   Line 1", 1010101100),
                    Arguments.of("", 0)
            );
        }

        @Test
        public void negative_convert_image_to_text_test_when_text_null() {
            String text = null;
            int linesPerPage = 3;

            Exception exception = assertThrows(TextIsNullException.class,
                    () -> service.convertTextToImage(text, linesPerPage));
            assertNotNull(exception, "Text should not be null");
        }
    }

    @Nested
    @DisplayName("Compress image test")
    class CompressImageTest {

        @Test
        void positive_compressImg_should_return_non_null_InputStream() {
            BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
            int[] size = {400, 300};

            InputStream result = service.compressImg(image, size);

            assertNotNull(result, "InputStream should not be null");
        }

        @Test
        void positive_compressImg_should_allow_reset_after_mark() throws Exception {

            BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
            int[] size = {400, 300};

            ByteArrayInputStream result = (ByteArrayInputStream) service.compressImg(image, size);

            byte[] buffer = new byte[1024];
            int bytesRead = result.read(buffer);

            assertTrue(bytesRead > 0, "Some bytes should be read initially");

            result.reset();
            bytesRead = result.read(buffer);

            assertTrue(bytesRead > 0, "Bytes should be read again after reset");
        }

        @Test
        void positive_compressImg_should_resize_image_correctly() throws Exception {

            BufferedImage originalImage = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
            int[] size = {400, 300};

            InputStream result = service.compressImg(originalImage, size);

            BufferedImage resizedImage = ImageIO.read(result);

            assertEquals(400, resizedImage.getWidth(), "Resized image width should be 400");
            assertEquals(300, resizedImage.getHeight(), "Resized image height should be 300");
        }

        @Test
        void negative_compressImg_should_throw_exception_when_image_is_null() {

            BufferedImage image = null;
            int[] size = {400, 300};

            Exception exception = assertThrows(ServerIOException.class, () -> {
                service.compressImg(image, size);
            });

            assertNotNull(exception, "Image should not be null");
        }

        @Test
        void negative_compressImg_should_throw_exception_when_size_is_invalid() {

            BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
            int[] invalidSize = {0, 0};

            Exception exception = assertThrows(ServerIOException.class, () -> {
                service.compressImg(image, invalidSize);
            });
            assertNotNull(exception, "Invalid size parameters");
        }
    }
}