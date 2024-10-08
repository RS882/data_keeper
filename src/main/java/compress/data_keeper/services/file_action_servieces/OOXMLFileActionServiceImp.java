package compress.data_keeper.services.file_action_servieces;

import compress.data_keeper.services.file_action_servieces.interfaces.FileActionService;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;

@Service
public class OOXMLFileActionServiceImp implements FileActionService {
    @Override
    public Map<String, InputStream> getFileImages(MultipartFile file) {

//        String fileType = null;
//        try {
//            new   XSSFWorkbook(file.getInputStream()).close();
//            fileType = "Excel";
//
//        } catch (Exception ignored) {
//        }
//
//        try {
//            if (fileType == null) {
//                new XMLSlideShow(file.getInputStream()).close();
//                fileType = "PowerPoint";
//            }
//        } catch (Exception ignored) {
//        }
//        try {
//            if (fileType == null) {
//                new XWPFDocument(file.getInputStream()).close();
//                fileType = "Word";
//            }
//        } catch (Exception ignored) {
//        }

        return Map.of();
    }
}
