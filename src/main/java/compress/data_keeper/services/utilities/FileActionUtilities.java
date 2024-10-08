package compress.data_keeper.services.utilities;

import compress.data_keeper.services.file_action_servieces.OOXMLFileActionServiceImp;
import compress.data_keeper.services.file_action_servieces.TextFileActionServiceImpl;
import compress.data_keeper.services.file_action_servieces.interfaces.FileActionService;
import org.springframework.http.MediaType;


public class FileActionUtilities {

    public static FileActionService getFileActionServiceByContentType(String contentType) {

        return switch (contentType) {
            case MediaType.TEXT_PLAIN_VALUE -> new TextFileActionServiceImpl();
            case "application/x-tika-ooxml"-> new OOXMLFileActionServiceImp();
            default -> null;
        };
    }
}
