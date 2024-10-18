package compress.data_keeper.services.utilities;

import compress.data_keeper.exception_handler.server_exception.ServerIOException;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileCalculators {

    public static String calculateHash(InputStream inputStream) {
        return calculateHash(inputStream, "SHA-256");
    }

    public static String calculateHash(InputStream inputStream, String algorithm) {
        if (inputStream == null) return "";

        try {
            inputStream.reset();
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
            inputStream.reset();
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new ServerIOException(e.getMessage());
        }
    }

    public static long calculateFileSize(InputStream inputStream) {
        if (inputStream == null) return 0;

        byte[] buffer = new byte[1024];
        int bytesRead;
        long size = 0;
        try {
            inputStream.reset();
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                size += bytesRead;
            }
            inputStream.reset();
            return size;
        } catch (IOException e) {
            throw new ServerIOException(e.getMessage());
        }
    }
}
