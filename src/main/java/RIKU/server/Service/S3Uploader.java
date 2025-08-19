package RIKU.server.Service;

import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.Exception.Domain.CustomException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import static RIKU.server.Util.BaseResponseStatus.MULTIPARTFILE_CONVERT_FAIL_IN_MEMORY;

@Slf4j
@RequiredArgsConstructor
@Service
public class S3Uploader {

    private final AmazonS3 amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${storage.s3.base-prefix}")
    private String basePrefix;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    public String upload(MultipartFile file, String relativePathOrDir) throws IOException {
        validateSize(file.getSize());

        String ext = getFileExtension(file.getOriginalFilename());
        String relative = ensureFileName(relativePathOrDir, ext);
        String key = resolveKey(relative);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        try (InputStream inputStream = file.getInputStream()) {
            log.info("[S3] putObject bucket={}, key={}", bucket, key);
            amazonS3Client.putObject(new PutObjectRequest(bucket, key, inputStream, metadata));
            return amazonS3Client.getUrl(bucket, key).toString();
        } catch (IOException e) {
            log.error("S3 upload 실패: key={}", key, e);
            throw new CustomException(MULTIPARTFILE_CONVERT_FAIL_IN_MEMORY);
        }
    }

    public void deleteFileByUrl(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);
            deleteByKey(key);
        } catch (MalformedURLException e) {
            log.error("URL 파싱 오류 : {}", fileUrl, e);
        }
    }

    private void validateSize(long size) {
        if (size > MAX_FILE_SIZE) {
            log.error("파일 크기 초과: {} bytes (limit={} bytes)", size, MAX_FILE_SIZE);
            throw new CustomException(BaseResponseStatus.FILE_SIZE_EXCEEDED);
        }
    }

    private void deleteByKey(String key) {
        if (amazonS3Client.doesObjectExist(bucket, key)) {
            amazonS3Client.deleteObject(bucket, key);
            log.info("[S3] 삭제 완료: {}", key);
        } else {
            log.warn("[S3] 삭제 대상 없음: {}", key);
        }
    }

    private String resolveKey(String relativePath) {
        String pfx = ensureTrailingSlash(nullToEmpty(basePrefix));
        String rel = stripLeadingSlash(nullToEmpty(relativePath));
        return pfx + rel;
    }

    private String ensureFileName(String relativePathOrDir, String ext) {
        String path = strip(relativePathOrDir);
        if (!path.contains(".")) {
            return (path.isEmpty() ? "" : path + "/") + UUID.randomUUID() + ext;
        }
        return path;
    }

    private String extractKeyFromUrl(String urlStr) throws MalformedURLException {
        URL url = new URL(urlStr);
        String path = url.getPath();
        return stripLeadingSlash(path);
    }

    private String getFileExtension(String fileName){
        if(fileName==null){
            return "";
        }
        int lastIndexOf = fileName.lastIndexOf(".");
        if(lastIndexOf==-1){
            return "";
        }
        return fileName.substring(lastIndexOf);
    }

    private static String strip(String s) {
        String t = nullToEmpty(s);
        return stripTrailingSlash(stripLeadingSlash(t));
    }

    private static String nullToEmpty(String s) { return (s == null) ? "" : s; }
    private static String stripLeadingSlash(String s) { return s.startsWith("/") ? s.substring(1) : s; }
    private static String stripTrailingSlash(String s) { return (s.endsWith("/")) ? s.substring(0, s.length()-1) : s; }
    private static String ensureTrailingSlash(String s) { return (s.endsWith("/")) ? s : (s + "/"); }
}
