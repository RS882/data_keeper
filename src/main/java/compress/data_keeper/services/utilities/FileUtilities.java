package compress.data_keeper.services.utilities;

import compress.data_keeper.exception_handler.bad_requeat.exceptions.BadFileFormatException;
import compress.data_keeper.exception_handler.bad_requeat.exceptions.BadFileSizeException;
import org.springframework.web.multipart.MultipartFile;

public class FileUtilities {

    public static  String toUnixStylePath(String path) {
        return path.replace("\\", "/");
    }

    public static void checkFile(MultipartFile file) {
        if (file.isEmpty()) throw new BadFileSizeException();
        if (file.getContentType().isEmpty())
            throw new BadFileFormatException(file.getOriginalFilename());
    }

    public static String getFileExtension(MultipartFile file) {
        String fileName = file.getOriginalFilename();

        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}
