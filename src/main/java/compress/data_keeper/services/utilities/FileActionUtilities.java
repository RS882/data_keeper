package compress.data_keeper.services.utilities;

import compress.data_keeper.services.file_action_servieces.ImageFileActionService;
import compress.data_keeper.services.file_action_servieces.OOXMLFileActionService;
import compress.data_keeper.services.file_action_servieces.TextFileActionService;
import compress.data_keeper.services.file_action_servieces.FileActionService;
import org.springframework.http.MediaType;


public class FileActionUtilities {

    public final static String OOXML_FILE_TYPE = "application/x-tika-ooxml";
    public final static String IMAGE_FILE_TYPE = "image";

    public static FileActionService getFileActionServiceByContentType(String contentType) {
        if (contentType == null) return null;
        if(contentType.toLowerCase().startsWith("image")) contentType=IMAGE_FILE_TYPE;
        return switch (contentType) {
            case MediaType.TEXT_PLAIN_VALUE -> new TextFileActionService();
            case OOXML_FILE_TYPE -> new OOXMLFileActionService();
            case IMAGE_FILE_TYPE-> new ImageFileActionService();
            default -> null;
        };
    }
}
