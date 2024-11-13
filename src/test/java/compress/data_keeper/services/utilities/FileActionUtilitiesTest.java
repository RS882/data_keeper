package compress.data_keeper.services.utilities;

import compress.data_keeper.services.file_action_servieces.OOXMLFileActionService;
import compress.data_keeper.services.file_action_servieces.TextFileActionService;
import compress.data_keeper.services.file_action_servieces.FileActionService;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import static compress.data_keeper.services.utilities.FileActionUtilities.OOXML_FILE_TYPE;
import static compress.data_keeper.services.utilities.FileActionUtilities.getFileActionServiceByContentType;
import static org.junit.jupiter.api.Assertions.*;

@DisplayNameGeneration(value = DisplayNameGenerator.ReplaceUnderscores.class)
class FileActionUtilitiesTest {


    @Nested
    @DisplayName("Tests for method getFileActionServiceByContentType")
    class Get_file_action_service_by_content_type_test {

        @Test
        public void get_TextFileActionService_when_type_TEXT_PLAIN() {
            String contentType = MediaType.TEXT_PLAIN_VALUE;

            FileActionService fileActionService = getFileActionServiceByContentType(contentType);
            assertNotNull(fileActionService);
            assertInstanceOf(TextFileActionService.class, fileActionService);
        }

        @Test
        public void get_OOXMLFileActionService_when_type_OOXML_FILE_TYPE() {
            String contentType = OOXML_FILE_TYPE;

            FileActionService fileActionService = getFileActionServiceByContentType(contentType);
            assertNotNull(fileActionService);
            assertInstanceOf(OOXMLFileActionService.class, fileActionService);
        }

        @Test
        public void get_null_when_type_undefined() {
            String contentType = "Test type";

            FileActionService fileActionService = getFileActionServiceByContentType(contentType);
            assertNull(fileActionService);
        }

        @Test
        public void get_null_when_type_null() {
            String contentType = null;

            FileActionService fileActionService = getFileActionServiceByContentType(contentType);
            assertNull(fileActionService);
        }
    }
}
