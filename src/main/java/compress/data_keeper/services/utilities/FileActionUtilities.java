package compress.data_keeper.services.utilities;

import compress.data_keeper.services.file_action_servieces.*;
import org.springframework.http.MediaType;


public class FileActionUtilities {

    public final static String OOXML_FILE_TYPE = "application/x-tika-ooxml";
    public final static String IMAGE_FILE_TYPE = "image";
    public final static String VIDEO_FILE_TYPE = "video";

    public static FileActionService getFileActionServiceByContentType(String contentType) {
        if (contentType == null) return null;
        if (contentType.toLowerCase().startsWith(IMAGE_FILE_TYPE)) {
            contentType = IMAGE_FILE_TYPE;
        }
        else if(contentType.toLowerCase().startsWith(VIDEO_FILE_TYPE)){
            contentType = VIDEO_FILE_TYPE;
        }
        return switch (contentType) {
            case MediaType.TEXT_PLAIN_VALUE -> new TextFileActionService();
            case OOXML_FILE_TYPE -> new OOXMLFileActionService();
            case IMAGE_FILE_TYPE -> new ImageFileActionService();
            case VIDEO_FILE_TYPE -> new VideoActionService();
            default -> null;
        };
    }
}
