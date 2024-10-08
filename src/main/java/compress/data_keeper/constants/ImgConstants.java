package compress.data_keeper.constants;

import java.util.Set;

public interface ImgConstants {
    int[] IMAGE_1024_SIZE = new int[]{1024, 1024};
    int[] IMAGE_320_SIZE = new int[]{320, 320};
    int[] IMAGE_64_SIZE = new int[]{64, 64};

    Set<int[]> IMAGE_SIZES = Set.of(IMAGE_1024_SIZE, IMAGE_320_SIZE, IMAGE_64_SIZE);
}
