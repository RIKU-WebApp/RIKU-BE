package RIKU.server.Service;

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


    // File에 저장하지 않고 Memory에서 변환 시행
    public String upload(MultipartFile file, String dirName) throws IOException {
        String originFileName = file.getOriginalFilename();
        String fileExtension = getFileExtension(originFileName);
        String uniqueFilename = generateUniqueFileName(dirName,fileExtension);

        //String fileName = dirName + "/" + file.getOriginalFilename();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        try (InputStream inputStream = file.getInputStream()) {
            log.info("Starting upload for file: {}", uniqueFilename);
            //amazonS3Client.putObject(new PutObjectRequest(bucket, uniqueFilename, inputStream, metadata));
            putS3( uniqueFilename, inputStream, metadata);
            log.info("File uploaded successfully: {}", uniqueFilename);
            return amazonS3Client.getUrl(bucket, uniqueFilename).toString();
        } catch (IOException e) {
            log.error("Error uploading file: {}", uniqueFilename, e);
            throw new CustomException(MULTIPARTFILE_CONVERT_FAIL_IN_MEMORY);
        }
    }

    private String generateUniqueFileName(String dirName ,String fileExtension){
        return dirName + "/" + UUID.randomUUID().toString() + fileExtension;
    }

    // 이미지 지우기
    public void deleteFileByUrl(String fileUrl) {
        // 예시: https://bucket.s3.amazonaws.com/dirName/profile.xxx
        // S3 URL 형식에 따라 객체 키를 추출하는 로직
        try {
            URL url = new URL(fileUrl);
            String path = url.getPath();        // 예시: /dirName/profile.xxx
            String key = path.startsWith("/") ? path.substring(1) : path;       // path에서 key 파싱

            if (amazonS3Client.doesObjectExist(bucket, key)) {
                amazonS3Client.deleteObject(bucket, key);
                log.info("S3에서 파일 삭제됨 : {}", key);
            } else {
                log.warn("삭제할 파일이 존재하지 않음 : {}", key);
            }
        } catch (MalformedURLException e) {
            log.error("URL 파싱 오류 : {}", fileUrl, e);
        }
    }

    // 업로드하기
    private String putS3(String fileName, InputStream uploadFile, ObjectMetadata metadata) {
        log.info("Uploading to S3 bucket: {}, file: {}", bucket, fileName);
        amazonS3Client.putObject(new PutObjectRequest(bucket, fileName, uploadFile,metadata));
        return amazonS3Client.getUrl(bucket, fileName).toString();
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
}
