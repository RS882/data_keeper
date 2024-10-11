package compress.data_keeper.services.utilities;

import compress.data_keeper.exception_handler.bad_requeat.exceptions.BadFileSizeException;
import compress.data_keeper.exception_handler.bad_requeat.exceptions.WrongSizeOfArrayException;
import org.springframework.web.multipart.MultipartFile;

public class FileUtilities {

    public static String toUnixStylePath(String path) {
        return path.replace("\\", "/");
    }

    public static String toWinStylePath(String path) {
        return path.replace("/", "\\");
    }

    public static void checkFile(MultipartFile file) {
        if (file.isEmpty()) throw new BadFileSizeException();
    }

    public static String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    public static String getNameFromSizes(int[] sizes) {
        if (sizes.length != 2) {
            throw new WrongSizeOfArrayException(2L);
        }
        return sizes[0] + "x" + sizes[1];
    }
}
