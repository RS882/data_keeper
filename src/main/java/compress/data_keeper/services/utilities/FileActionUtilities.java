package compress.data_keeper.services.utilities;

import compress.data_keeper.services.file_action_servieces.OOXMLFileActionServiceImp;
import compress.data_keeper.services.file_action_servieces.TextFileActionServiceImpl;
import compress.data_keeper.services.file_action_servieces.FileActionService;
import org.springframework.http.MediaType;


public class FileActionUtilities {

    public final static String OOXML_FILE_TYPE = "application/x-tika-ooxml";

    public static FileActionService getFileActionServiceByContentType(String contentType) {
        if (contentType == null) return null;
        return switch (contentType) {
            case MediaType.TEXT_PLAIN_VALUE -> new TextFileActionServiceImpl();
            case OOXML_FILE_TYPE -> new OOXMLFileActionServiceImp();
            default -> null;
        };
    }
}
