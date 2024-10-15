package compress.data_keeper.services.utilities;

import compress.data_keeper.exception_handler.server_exception.ServerIOException;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static compress.data_keeper.services.utilities.FileCalculators.calculateFileSize;
import static compress.data_keeper.services.utilities.FileCalculators.calculateHash;
import static org.junit.jupiter.api.Assertions.*;

@DisplayNameGeneration(value = DisplayNameGenerator.ReplaceUnderscores.class)
class FileCalculatorsTest {

    @Nested
    @DisplayName("Tests of calculateHash method")
    class CalculateHash_tests {

        @Test
        public void calculate_hash_when_algorithm_is_SHA_256() {
            String algorithm = "SHA-256";
            InputStream inputStream = new ByteArrayInputStream("Hello World".getBytes());

            String fileHash = calculateHash(inputStream, algorithm);

            assertNotNull(fileHash);
            assertNotEquals("", fileHash);
        }

        @Test
        public void calculate_hash_when_algorithm_is_MD5() {
            String algorithm = "MD5";
            InputStream inputStream = new ByteArrayInputStream("Hello World".getBytes());

            String fileHash = calculateHash(inputStream, algorithm);

            assertNotNull(fileHash);
            assertNotEquals("", fileHash);
        }

        @Test
        public void calculate_hash_when_input_is_nullv() throws Exception {
            String algorithm = "SHA-256";
            InputStream inputStream = null;

            String expectedHash = "";
            String actualHash = calculateHash(inputStream, algorithm);

            assertEquals(expectedHash, actualHash);
        }

        @Test
        public void calculate_hash_return_exception_when_algorithm_is_invalid() {
            InputStream inputStream = new ByteArrayInputStream("Hello World".getBytes());

            assertThrows(ServerIOException.class, () -> {
                calculateHash(inputStream, "INVALID_ALGO");
            });
        }
    }

    @Nested
    @DisplayName("Test of calculateFileSize method")
    class CalculateFileSize_tests {

        @Test
        public void calculateFileSize_when_input_is_correct() throws Exception {
            InputStream inputStream = new ByteArrayInputStream("Hello World".getBytes());

            long fileSize = calculateFileSize(inputStream);

            assertNotNull(fileSize);
            assertTrue(fileSize > 0);
        }

        @Test
        public void calculateFileSize_when_input_is_null() throws Exception {
            InputStream inputStream = null;

            long fileSize = calculateFileSize(inputStream);

            assertNotNull(fileSize);
            assertEquals(0, fileSize);
        }
    }
}
