package neox.video.services;

import lombok.extern.slf4j.Slf4j;
import neox.video.constants.VideoProperties;
import neox.video.domain.dto.VideoPropsDto;
import neox.video.exception_handler.bad_requeat.exceptions.BadFileFormatException;
import neox.video.exception_handler.server_exception.exceptions.UploadException;
import neox.video.exception_handler.server_exception.exceptions.VideoCompessException;
import neox.video.services.interfaces.VideoService;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;


import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.Videoio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Array;
import java.util.Arrays;
import java.util.UUID;

import static org.bytedeco.opencv.global.opencv_imgproc.resize;

@Service
@Slf4j
public class VideoServiceImpl implements VideoService {

    @Value("${data.temp-folder}")
    private String tempFolder;
    @Value("${data.folder}")
    private String videoFolder;

    @Override
    public void save(MultipartFile file, VideoProperties quality) {

        UUID uuid = UUID.randomUUID();
        String fileName = uuid + ".mp4";
        Path tempDir = Paths.get(tempFolder, uuid.toString());
        Path inputVideoPath = tempDir.resolve(fileName);
        Path outputVideoPath = tempDir.resolve("compressed_" + fileName);

        try {
            Files.createDirectory(tempDir);
            Files.copy(file.getInputStream(), inputVideoPath);
            compress(inputVideoPath.toString(), outputVideoPath.toString(), file.getOriginalFilename(), quality);
        } catch (IOException e) {
            log.error("Video didn't upload:{} ", e.getMessage());
            throw new UploadException(file.getOriginalFilename());
        }
    }


    @Override
    public void compress(String inputFile, String outputFile,
                         String originalFileName, VideoProperties quality) {

//        FFmpegLogCallback.set();
//        avutil.av_log_set_level(avutil.AV_LOG_TRACE);

        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputFile);
        FFmpegFrameRecorder recorder = null;

        try {
            grabber.start();

            int width = grabber.getImageWidth();
            int height = grabber.getImageHeight();
            int targetWidth = width;
            int targetHeight = height;
            double frameRate = grabber.getFrameRate();
//            int vbt = grabber.getVideoBitrate();
//            int abt = grabber.getAudioBitrate();
//            log.info("VideoBitrate {}", vbt);
//            log.info("AudioBitrate {}", abt);

            boolean[] areBitrates = areVideoBitratesValid(
                    originalFileName,
                    quality,
                    VideoPropsDto.builder()
                            .audioBitrate(grabber.getAudioBitrate())
                            .videoBitrate(grabber.getVideoBitrate())
                            .width(width)
                            .height(height)
                            .frameRate(frameRate)
                            .build()
            );

            recorder = new FFmpegFrameRecorder(outputFile, targetWidth, targetHeight);
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setFormat("mp4");
            recorder.setFrameRate(frameRate);
            recorder.setVideoBitrate(quality.getVideoProps().getVideoBitrate());

            recorder.setAudioChannels(grabber.getAudioChannels());
            recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
            recorder.setAudioBitrate(quality.getVideoProps().getAudioBitrate());

            recorder.start();
            Frame frame;
            while ((frame = grabber.grab()) != null) {
                recorder.record(frame);
            }
//            resizeVideo(grabber, recorder, targetWidth, targetHeight);


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
            } catch (Exception e) {
                log.error("Video compress  exception:{} ", e.getMessage());
                throw new VideoCompessException(originalFileName);
            }
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

    private boolean isVideoMp4(String path, String originalFileName) {
        try (InputStream inputStream = new FileInputStream(path)) {
            byte[] magicBytes = new byte[12];
            inputStream.read(magicBytes);

            byte[] mp4MagicBytes = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x20, (byte) 0x66, (byte) 0x74, (byte) 0x79, (byte) 0x70, (byte) 0x69, (byte) 0x73, (byte) 0x6F, (byte) 0x6D};

            return Arrays.equals(magicBytes, mp4MagicBytes);

        } catch (IOException e) {
            throw new BadFileFormatException(originalFileName);
        }
    }

    private boolean[] areVideoBitratesValid(String fileName, VideoProperties quality, VideoPropsDto currentProps) {
        VideoPropsDto qualityProps = quality.getVideoProps();
        boolean[] areBitratesValid = {true, true};

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
            areBitratesValid[0] = false;
        }
        if (currentProps.getAudioBitrate() > qualityProps.getAudioBitrate()) {
            log.warn("Audio {} bitrate {} is greater than Audio bitrate {} for :{}," +
                            "video will be modified",
                    fileName,
                    currentProps.getAudioBitrate(),
                    qualityProps.getAudioBitrate(),
                    quality.name());
            areBitratesValid[1] = false;
        }
        return areBitratesValid;
    }


}

