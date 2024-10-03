package compress.data_keeper.services.utilities;

import compress.data_keeper.exception_handler.server_exception.ServerIOException;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileHashCalculator {

    public static String calculateHash(InputStream inputStream) {
        return calculateHash(inputStream, "SHA-256");
    }

    public static String calculateHash(InputStream inputStream, String algorithm) {
        try {

            MessageDigest digest = MessageDigest.getInstance(algorithm);

            byte[] byteArray = new byte[1024];
            int bytesCount;

            while ((bytesCount = inputStream.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }

            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new ServerIOException(e.getMessage());
        }
    }
}
