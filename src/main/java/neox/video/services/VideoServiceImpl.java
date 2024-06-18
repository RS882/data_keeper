package neox.video.services;

import lombok.extern.slf4j.Slf4j;
import neox.video.constants.VideoProperties;
import neox.video.domain.dto.VideoPropsDto;
import neox.video.domain.dto.VideoResponseDto;
import neox.video.exception_handler.bad_requeat.exceptions.BadFileFormatException;
import neox.video.exception_handler.bad_requeat.exceptions.BadFileSizeException;
import neox.video.exception_handler.server_exception.exceptions.UploadException;
import neox.video.exception_handler.server_exception.exceptions.VideoCompessException;
import neox.video.services.interfaces.VideoService;
import org.bytedeco.ffmpeg.global.avcodec;


import org.bytedeco.javacv.*;


import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.*;

import static org.bytedeco.opencv.global.opencv_imgproc.resize;

@Service
@Slf4j
public class VideoServiceImpl implements VideoService {

    @Value("${data.temp-folder}")
    private String tempFolder;

    @Override
    public VideoResponseDto save(MultipartFile file, VideoProperties quality) {

        checkFile(file, quality);

        UUID uuid = UUID.randomUUID();

        Path tempDir = Paths.get(tempFolder, uuid.toString());
        Path outputVideoPath = tempDir.resolve(uuid + ".mp4");
        Path previewPicturePath = tempDir.resolve(uuid + ".jpeg");

        try {
            Path inputVideoPath = tempDir.resolve(
                    Objects.requireNonNull(file.getOriginalFilename()));
            Files.createDirectory(tempDir);
            Files.copy(file.getInputStream(), inputVideoPath);

            compress(
                    inputVideoPath,
                    outputVideoPath,
                    file.getOriginalFilename(),
                    quality);

            savePreviewPictures(outputVideoPath,
                    previewPicturePath,
                    file.getOriginalFilename());

            return getPathsMap(outputVideoPath,previewPicturePath,quality);
        } catch (IOException e) {
            log.error("Video didn't upload:{} ", e.getMessage());
            throw new UploadException(
                    String.format("The video file %s cannot be downloaded",
                            file.getOriginalFilename()));
        }
    }


    @Override
    public void compress(Path inputFile,
                         Path outputFile,
                         String originalFileName,
                         VideoProperties quality) {
//        FFmpegLogCallback.set();
//        avutil.av_log_set_level(avutil.AV_LOG_TRACE);

        FFmpegFrameGrabber g = new FFmpegFrameGrabber(inputFile.toString());
        FFmpegFrameRecorder recorder = null;

        try {
            g.start();

            int width = g.getImageWidth();
            int height = g.getImageHeight();
            int targetWidth = width;
            int targetHeight = height;
            double frameRate = g.getFrameRate();

            boolean areBitratesValid = areVideoBitratesValid(
                    originalFileName,
                    quality,
                    VideoPropsDto.builder()
                            .audioBitrate(g.getAudioBitrate())
                            .videoBitrate(g.getVideoBitrate())
                            .width(width)
                            .height(height)
                            .frameRate(frameRate)
                            .build()
            );

            if (areBitratesValid) {
                g.stop();
                Files.move(inputFile, outputFile);
            } else {
                recorder = new FFmpegFrameRecorder(
                        outputFile.toString(),
                        targetWidth,
                        targetHeight);

                recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                recorder.setFormat("mp4");
                recorder.setFrameRate(frameRate);
                recorder.setVideoBitrate(quality.getVideoProps().getVideoBitrate());

                recorder.setAudioChannels(g.getAudioChannels());
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
                recorder.setAudioBitrate(quality.getVideoProps().getAudioBitrate());

                recorder.start();

                Frame frame;

                while ((frame = g.grab()) != null) {
                    recorder.record(frame);

                }
//            resizeVideo(g, recorder, targetWidth, targetHeight);
            }
        } catch (Exception e) {
            log.error("Video compress  exception:{} ", e.getMessage());
            throw new VideoCompessException(originalFileName);
        } finally {
            try {
                if (g != null) {
                    g.stop();
                    g.release();
                }
                if (recorder != null) {
                    recorder.stop();
                    recorder.release();
                }
                if (Files.exists(inputFile)) {
                    Files.delete(inputFile);
                    log.info("Existing input file <{}> deleted.", inputFile);
                }
            } catch (Exception e) {
                log.error("Video compress  exception:{} ", e.getMessage());
                throw new VideoCompessException(originalFileName);
            }
        }
    }

    private static void savePreviewPictures(Path filePath, Path picturePath, String originalFilename) {
        String filePathStr = filePath.toString();

        try (FFmpegFrameGrabber g = new FFmpegFrameGrabber(filePathStr)) {
            try (Java2DFrameConverter converter = new Java2DFrameConverter()) {

                g.start();
                BufferedImage image;
                for (int i = 0; i < 50; i++) {
                    image = converter.convert(g.grabKeyFrame());
                    if (image != null) {
                        File file = picturePath.toFile();
                        ImageIO.write(image, "jpeg", file);
                        break;
                    }
                }
            }
            g.stop();
        } catch (Exception e) {
            throw new UploadException(
                    String.format("The preview pictute for file  %s cannot be saved", originalFilename));
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

    private boolean areVideoBitratesValid(String fileName, VideoProperties quality, VideoPropsDto currentProps) {
        VideoPropsDto qualityProps = quality.getVideoProps();

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
            log.warn("Video {} bitrate {} is greater than video bitrate {} for :{}," +
                            "video will be modified",
                    fileName,
                    currentProps.getVideoBitrate(),
                    qualityProps.getVideoBitrate(),
                    quality.name());
            return false;
        }
        if (currentProps.getAudioBitrate() > qualityProps.getAudioBitrate()) {
            log.warn("Audio {} bitrate {} is greater than Audio bitrate {} for :{}," +
                            "video will be modified",
                    fileName,
                    currentProps.getAudioBitrate(),
                    qualityProps.getAudioBitrate(),
                    quality.name());
            return false;
        }
        return true;
    }

    private VideoResponseDto getPathsMap(Path video, Path preview,VideoProperties quality){
        Map<String,String> paths = new HashMap<>();
        paths.put("video",toUnixStylePath(video.toString()));
        paths.put("preview",toUnixStylePath(preview.toString()));
        Map<VideoProperties, Map<String, String>> dto = new HashMap<>();
        dto.put(quality, paths);

        return  new VideoResponseDto(dto);
    }
    private String toUnixStylePath(String path) {
        return path.replace("\\", "/");
    }

}

