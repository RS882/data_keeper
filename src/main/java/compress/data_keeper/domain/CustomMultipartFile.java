package compress.data_keeper.domain;

import compress.data_keeper.exception_handler.server_exception.ServerIOException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

@AllArgsConstructor
@Builder
public class CustomMultipartFile implements MultipartFile {
    private final byte[] content;
    private final String name;
    private final String originalFilename;
    private final String contentType;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return content;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        try (OutputStream outputStream = new FileOutputStream(dest)) {
            outputStream.write(content);
        }
    }

    public static MultipartFile toCustomMultipartFile(MultipartFile file) {
        String contentType = file.getContentType();
        try {
            return CustomMultipartFile.builder()
                    .name(file.getName())
                    .originalFilename(file.getOriginalFilename())
                    .contentType(
                            contentType == null || contentType.isEmpty() ?
                                    MediaType.APPLICATION_OCTET_STREAM_VALUE :
                                    contentType)
                    .content(file.getBytes())
                    .build();
        } catch (IOException e) {
            throw new ServerIOException(e.getMessage());
        }
    }
}
