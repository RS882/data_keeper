package compress.video.exception_handler.not_found.exceptions;

import compress.video.exception_handler.not_found.NotFoundException;

public class VideoPropertiesNotFoundException extends NotFoundException {
    public VideoPropertiesNotFoundException(String quality) {
        super(String.format("Video properties for : <%s> not found", quality));
    }
}
