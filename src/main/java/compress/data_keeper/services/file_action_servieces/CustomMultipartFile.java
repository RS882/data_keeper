package compress.data_keeper.services.file_action_servieces;

import lombok.AllArgsConstructor;
import lombok.Builder;
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
}
