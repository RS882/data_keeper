package compress.data_keeper.services.utilities;

import compress.data_keeper.exception_handler.bad_requeat.exceptions.BadFileSizeException;
import compress.data_keeper.exception_handler.bad_requeat.exceptions.WrongSizeOfArrayException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.multipart.MultipartFile;

import java.util.stream.Stream;

import static compress.data_keeper.services.utilities.FileUtilities.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayNameGeneration(value = DisplayNameGenerator.ReplaceUnderscores.class)
class FileUtilitiesTest {

    @Nested
    @DisplayName("Tests of toUnixStylePath method")
    class ToUnixStylePath_tests {

        @Test
        public void toUnixStylePath_when_path_is_correct() {
            String path = "test\\test\\test\\test\\test\\test\\test\\test\\test\\test";
            String expectedPath = "test/test/test/test/test/test/test/test/test/test";
            String result = toUnixStylePath(path);
            assertEquals(expectedPath, result);
        }

        @Test
        public void toUnixStylePath_when_path_is_null() {
            String path = null;
            String result = toUnixStylePath(path);
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("Tests of toWinStylePath method")
    class ToWinStylePath_tests {

        @Test
        public void toUnixStylePath_when_path_is_correct() {
            String path = "test/test/test/test/test/test/test/test/test/test";
            String expectedPath = "test\\test\\test\\test\\test\\test\\test\\test\\test\\test";
            String result = toWinStylePath(path);
            assertEquals(expectedPath, result);
        }

        @Test
        public void toUnixStylePath_when_path_is_null() {
            String path = null;
            String result = toWinStylePath(path);
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("Tests of checkFile method")
    class CheckFile_tests {

        @Test
        public void checkFile_when_file_isnt_empty() {
            MultipartFile file = mock(MultipartFile.class);
            checkFile(file);
        }

        @Test
        public void checkFile_return_exception_when_file_is_empty() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(true);
            assertThrows(BadFileSizeException.class, () -> {
                checkFile(file);
            });
        }
    }

    @Nested
    @DisplayName("Tests of getFileExtension method")
    class GetFileExtension_tests {

        @Test
        public void getFileExtension_when_fileName_is_correct() {
            String fileName = "test.txt";
            String expectedExtension = ".txt";
            String result = getFileExtension(fileName);
            assertEquals(expectedExtension, result);
        }

        @Test
        public void getFileExtension_when_fileName_is_null() {
            String fileName = null;
            String expectedExtension = "";
            String result = getFileExtension(fileName);
            assertEquals(expectedExtension, result);
        }

        @Test
        public void getFileExtension_when_fileName_doesnt_contains_dot() {
            String fileName = "test_name";
            String expectedExtension = "";
            String result = getFileExtension(fileName);
            assertEquals(expectedExtension, result);
        }
    }

    @Nested
    @DisplayName("Tests of getNameFromSizes method")
    class GetNameFromSizes_tests {

        @Test
        public void getNameFromSizes_when_sizes_is_correct() {
            int[] size = {22, 33};
            String sizes = "22x33";
            String result = getNameFromSizes(size);
            assertEquals(sizes, result);
        }

        @ParameterizedTest(name = "Test {index}: getNameFromSizes_return_exception_when_sizes_length_doesnt_equals_2 [{arguments}]")
        @MethodSource("wrongSizeArrays")
        public void getNameFromSizes_return_exception_when_sizes_length_doesnt_equals_2(int[] size) {

            assertThrows(WrongSizeOfArrayException.class,
                    () -> getNameFromSizes(size));
        }

        private static Stream<Arguments> wrongSizeArrays() {
            return Stream.of(
                    Arguments.of(new int[]{22, 33, 55, 77}),
                    Arguments.of(new int[]{22}));
        }
    }
}
