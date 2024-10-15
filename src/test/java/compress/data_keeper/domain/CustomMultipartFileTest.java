package compress.data_keeper.domain;

import compress.data_keeper.exception_handler.server_exception.ServerIOException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static compress.data_keeper.domain.CustomMultipartFile.toCustomMultipartFile;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayNameGeneration(value = DisplayNameGenerator.ReplaceUnderscores.class)
class CustomMultipartFileTest {

    @Test
    public void positive_get_CustomMultipartFile_test_with_all_parameter() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Hello World".getBytes());
        MultipartFile res = toCustomMultipartFile(file);

        assertNotNull(res);
        assertEquals(file.getOriginalFilename(), res.getOriginalFilename());
        assertEquals(file.getContentType(), res.getContentType());
        assertEquals(file.getSize(), res.getSize());
        assertEquals(file.getBytes(), res.getBytes());
    }

    @Test
    public void positive_get_CustomMultipartFile_test_with_content_type_is_null() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                null,
                "Hello World".getBytes());
        MultipartFile res = toCustomMultipartFile(file);

        assertNotNull(res);
        assertEquals(file.getOriginalFilename(), res.getOriginalFilename());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM_VALUE, res.getContentType());
        assertEquals(file.getSize(), res.getSize());
        assertEquals(file.getBytes(), res.getBytes());
    }

    @Test
    public void positive_get_CustomMultipartFile_test_with_content_type_is_empty() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "",
                "Hello World".getBytes());
        MultipartFile res = toCustomMultipartFile(file);

        assertNotNull(res);
        assertEquals(file.getOriginalFilename(), res.getOriginalFilename());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM_VALUE, res.getContentType());
        assertEquals(file.getSize(), res.getSize());
        assertEquals(file.getBytes(), res.getBytes());
    }

    @Test
    public void negative_get_CustomMultipartFile_test_when_IOException() throws Exception {
        MultipartFile mockFile =mock(MultipartFile.class);
        when(mockFile.getBytes()).thenThrow(new IOException());

       Exception exception = assertThrows(ServerIOException.class,
               () -> toCustomMultipartFile(mockFile));

        assertNotNull(exception);
    }
}

