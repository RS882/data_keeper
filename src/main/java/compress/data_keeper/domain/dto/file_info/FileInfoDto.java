package compress.data_keeper.domain.dto.file_info;

import compress.data_keeper.domain.entity.Folder;
import compress.data_keeper.exception_handler.server_exception.ServerIOException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static compress.data_keeper.constants.MediaFormats.IMAGE_FORMAT;
import static compress.data_keeper.services.utilities.FileCalculators.calculateFileSize;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileInfoDto {
    private UUID fileId;
    private String bucketName;
    private String fileName;
    private String fileDescription;
    private Long fileSize;
    private Folder fileFolder;
    private InputStream inputStream;
    private String fileType;

    private String filePath;
    private Boolean isOriginalFile;

    public FileInfoDto(
            MultipartFile file,
            Folder folder,
            String fileDescription) {
        this.fileName = file.getOriginalFilename();
        this.fileDescription = fileDescription;
        this.fileFolder = folder;
        this.fileSize = file.getSize();
        this.fileType = file.getContentType();
        try {
            this.inputStream = file.getInputStream();
        } catch (IOException e) {
            throw new ServerIOException(e.getMessage());
        }
    }

    public FileInfoDto(
            InputStream inputStream,
            Folder folder,
            String pathOfFile,
            String fileDescription) {
        this.fileName = fileDescription + "." + IMAGE_FORMAT;
        this.fileDescription = fileDescription;
        this.fileFolder = folder;
        this.fileSize = calculateFileSize(inputStream);
        this.fileType = MediaType.IMAGE_JPEG_VALUE;
        this.filePath = pathOfFile;
        this.isOriginalFile = false;
        this.inputStream = inputStream;
    }
}
