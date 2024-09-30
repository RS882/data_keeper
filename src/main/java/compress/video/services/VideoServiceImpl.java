package compress.video.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import compress.video.constants.MediaFormats;
import compress.video.constants.VideoProperties;
import compress.video.domain.dto.VideoPropsDto;
import compress.video.domain.dto.VideoResponseDto;
import compress.video.exception_handler.bad_requeat.exceptions.BadFileFormatException;
import compress.video.exception_handler.bad_requeat.exceptions.BadFileSizeException;
import compress.video.exception_handler.server_exception.exceptions.UploadException;
import compress.video.exception_handler.server_exception.exceptions.VideoCompessException;
import compress.video.exception_handler.server_exception.exceptions.MinioGetUrlException;
import compress.video.services.interfaces.DataStorageService;
import compress.video.services.interfaces.VideoService;
import org.bytedeco.ffmpeg.global.avcodec;

import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static compress.video.services.utilities.FileUtilities.toUnixStylePath;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService, MediaFormats {

    @Value("${data.temp-folder}")
    private String tempFolder;

    @Value("${bucket.name}")
    private String bucketName;

    @Value("${storage.url}")
    private String storageUrl;

    @Value("${prefix.dir}")
    private String dirPrefix;

    @Value("${prefix.private}")
    private String prefixPrivate;

    @Value("${prefix.public}")
    private String prefixPublic;

    private final DataStorageService dataStorageService;

    @Override
    public VideoResponseDto save(
            MultipartFile file,
            VideoProperties quality,
            boolean isPublic) {

        checkFile(file, quality);

        UUID uuid = UUID.randomUUID();

        String videoFileName = uuid + "." + VIDEO_FORMAT;
        String previewPictureName = uuid + "." + IMAGE_FORMAT;

        Path tempDir = Paths.get(tempFolder, uuid.toString());
        Path tempOutputVideoPath = tempDir.resolve(videoFileName);

        Path path = Path.of(dirPrefix, isPublic ? prefixPublic : prefixPrivate + uuid);
        Path outputVideoPath = path.resolve(videoFileName);
        Path previewPicturePath = path.resolve(previewPictureName);

        try {
            Files.createDirectory(tempDir);

            compress(file, tempOutputVideoPath, quality, outputVideoPath);

            savePreviewPictures(tempOutputVideoPath,
                    previewPicturePath,
                    file.getOriginalFilename());

            if (Files.exists(tempOutputVideoPath)) {
                Files.delete(tempOutputVideoPath);
                Files.delete(tempDir);
                log.info("Existing input file {} deleted.", tempOutputVideoPath);
            }

            return getPathsMap(outputVideoPath, previewPicturePath, quality, isPublic);

        } catch (IOException e) {
            log.error("Video didn't upload:{} ", e.getMessage());
            throw new UploadException(
                    String.format("The video file %s cannot be downloaded",
                            file.getOriginalFilename()));
        }
    }

    public void compress(MultipartFile file,
                         Path tempOutputFile,
                         VideoProperties quality,
                         Path outputFile) {
//        FFmpegLogCallback.set();
//        avutil.av_log_set_level(avutil.AV_LOG_TRACE);

        String originalFileName = file.getOriginalFilename();
        FFmpegFrameGrabber grabber = null;
        FFmpegFrameRecorder recorder = null;

        try {
            grabber = new FFmpegFrameGrabber(file.getInputStream());

            grabber.start();

            int width = grabber.getImageWidth();
            int height = grabber.getImageHeight();
            int targetWidth = width;
            int targetHeight = height;
            double frameRate = grabber.getFrameRate();
            int videoBitrate = grabber.getVideoBitrate();
            int audioBitrate = grabber.getAudioBitrate();

            boolean[] areBitratesValid = areVideoBitratesValid(
                    originalFileName,
                    quality,
                    VideoPropsDto.builder()
                            .audioBitrate(audioBitrate)
                            .videoBitrate(videoBitrate)
                            .width(width)
                            .height(height)
                            .frameRate(frameRate)
                            .build()
            );

            if (areBitratesValid[0] && areBitratesValid[1]) {
                grabber.stop();
                Files.copy(file.getInputStream(), tempOutputFile);
            } else {
                recorder = new FFmpegFrameRecorder(
                        tempOutputFile.toString(),
                        targetWidth,
                        targetHeight);

                recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                recorder.setFormat(VIDEO_FORMAT);
                recorder.setFrameRate(frameRate);
                recorder.setVideoBitrate(
                        areBitratesValid[0] ?
                                videoBitrate :
                                quality.getVideoProps().getVideoBitrate());

                recorder.setAudioChannels(grabber.getAudioChannels());
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
                recorder.setAudioBitrate(
                        areBitratesValid[1] ?
                                audioBitrate :
                                quality.getVideoProps().getAudioBitrate());
                recorder.start();

                Frame frame;
                while ((frame = grabber.grab()) != null) {
                    recorder.record(frame);
                }
//            resizeVideo(grabber, recorder, targetWidth, targetHeight);
            }
        } catch (Exception e) {
            log.error("Video compress  exception:{} ", e.getMessage());
            throw new VideoCompessException(originalFileName);
        } finally {
            try {
                if (grabber != null) {
                    grabber.stop();
                    grabber.release();
                }
                if (recorder != null) {
                    recorder.stop();
                    recorder.release();
                }
                dataStorageService.uploadFIle(tempOutputFile.toString(), outputFile.toString());

            } catch (Exception e) {
                log.error("Video compress  exception:{} ", e.getMessage());
                throw new VideoCompessException(originalFileName);
            }
        }
    }


    private void savePreviewPictures(Path filePath, Path picturePath, String originalFilename) {
        String filePathStr = filePath.toString();

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(filePathStr)) {
            BufferedImage image = null;
            try (Java2DFrameConverter converter = new Java2DFrameConverter()) {

                grabber.start();

                for (int i = 0; i < 50; i++) {
                    image = converter.convert(grabber.grabKeyFrame());
                    if (image != null) {
                        break;
                    }
                }
            }
            grabber.stop();

            if (image != null) {

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, IMAGE_FORMAT, baos);
                baos.flush();

                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

                dataStorageService.uploadFIle(bais, picturePath.toString());

            }
        } catch (Exception e) {
            throw new UploadException(
                    String.format("The preview picture for file  %s cannot be saved", originalFilename));
        }
    }

    private void resizeVideo(FFmpegFrameGrabber grabber,
                             FFmpegFrameRecorder recorder,
                             int width,
                             int height)
            throws FFmpegFrameGrabber.Exception, FFmpegFrameRecorder.Exception {
        Frame frame;
        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
        Mat resizeMat = new Mat();

        while ((frame = grabber.grab()) != null) {
            if (frame.image != null) {
                Mat mat = converter.convert(frame);
                if (mat != null && !mat.empty()) {
                    resize(mat, resizeMat, new Size(width, height));

                    recorder.setImageWidth(resizeMat.cols());
                    recorder.setImageHeight(resizeMat.rows());

                    Frame resizedFrame = converter.convert(resizeMat);
                    recorder.record(resizedFrame);

                    mat.release();
                }
            }
            if (!resizeMat.empty()) {
                resizeMat.release();
            }
        }
    }

    private void checkFile(MultipartFile file, VideoProperties quality) {

        if (file.isEmpty()) throw new BadFileSizeException();

        if (!file.getContentType().startsWith("video/"))
            throw new BadFileFormatException(file.getOriginalFilename());

        if (file.getSize() > quality.getVideoProps().getMaxSize())
            throw new BadFileSizeException(
                    file.getOriginalFilename(),
                    file.getSize(),
                    quality.getVideoProps().getMaxSize());
    }

    private boolean[] areVideoBitratesValid(String fileName, VideoProperties quality, VideoPropsDto currentProps) {
        VideoPropsDto qualityProps = quality.getVideoProps();
        boolean[] result = {true, true};
        if (currentProps.getWidth() > qualityProps.getWidth()) {
            log.warn("Video {} width {} is greater than video width {} for :{}",
                    fileName,
                    currentProps.getWidth(),
                    qualityProps.getWidth(),
                    quality.name());
        }
        if (currentProps.getHeight() > qualityProps.getHeight()) {
            log.warn("Video {} height {} is greater than video height {} for :{}",
                    fileName,
                    currentProps.getHeight(),
                    qualityProps.getHeight(),
                    quality.name());
        }
        if (currentProps.getFrameRate() > qualityProps.getFrameRate()) {
            log.warn("Video {} frame rete {} is greater than video frame rete {} for :{}",
                    fileName,
                    currentProps.getFrameRate(),
                    qualityProps.getFrameRate(),
                    quality.name());
        }
        if (currentProps.getFrameRate() > qualityProps.getFrameRate()) {
            log.warn("Video {} frame rete {} is greater than video frame rete {} for :{}",
                    fileName,
                    currentProps.getFrameRate(),
                    qualityProps.getFrameRate(),
                    quality.name());
        }
        if (currentProps.getVideoBitrate() > qualityProps.getVideoBitrate()) {
            log.warn("Video {} bitrate {} is greater than video bitrate {} for :{}. " +
                            "Video will be modified",
                    fileName,
                    currentProps.getVideoBitrate(),
                    qualityProps.getVideoBitrate(),
                    quality.name());
            result[0] = false;
        }
        if (currentProps.getAudioBitrate() > qualityProps.getAudioBitrate()) {
            log.warn("Audio {} bitrate {} is greater than Audio bitrate {} for :{}. " +
                            "Video will be modified",
                    fileName,
                    currentProps.getAudioBitrate(),
                    qualityProps.getAudioBitrate(),
                    quality.name());
            result[1] = false;
        }
        return result;
    }

    private VideoResponseDto getPathsMap(
            Path video,
            Path preview,
            VideoProperties quality,
            boolean isPublic) {

        String postfix = isPublic ? "" : "_temp";

        Map<String, String> paths = new HashMap<>();
        paths.put("video" + postfix, getFullPath(video, isPublic));
        paths.put("preview" + postfix, getFullPath(preview, isPublic));

        Map<VideoProperties, Map<String, String>> dto = new HashMap<>();
        dto.put(quality, paths);

        return new VideoResponseDto(dto);
    }

    private String getFullPath(Path path, boolean isPublic) {
        try {
            return isPublic ?
                    toUnixStylePath( storageUrl+ "/" + bucketName + "/" + path) :
                    dataStorageService.getTempFullPath(path.toString());
        } catch (Exception e) {
            throw new MinioGetUrlException("something went wrong...");
        }
    }


}

