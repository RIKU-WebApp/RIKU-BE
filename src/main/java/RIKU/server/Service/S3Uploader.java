package RIKU.server.Service;

import RIKU.server.Util.AllowedImageFileExtensions;
import RIKU.server.Util.Exception.Domain.CustomException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import static RIKU.server.Util.BaseResponseStatus.MULTIPARTFILE_CONVERT_FAIL_IN_MEMORY;

@Slf4j
@RequiredArgsConstructor
@Service
public class S3Uploader {
    private final AmazonS3 amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    //MultipartFile을 전달 받아 File로 전환 후 S3 업로드
//    public String uploadFile(MultipartFile multipartFile, String dirName) throws IOException{// dirName의 디렉토리가 S3 Bucket 내부에 생성됨
//
//        File uploadFile = convert(multipartFile).orElseThrow(()-> new IllegalArgumentException("MultipartFile -> File 전환 실패"));
//        //System.out.p
//        // print("error: multipart file input. cant control");
//        return upload(uploadFile,dirName);
//    }

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

    //    public String upload(File uploadFile, String dirName){
//        String fileName = dirName+"/"+uploadFile.getName();
//        String uploadImageUrl = putS3(uploadFile,fileName);
//
//        removeNewFile(uploadFile);// convert()함수로 인해서 로컬에 생성된 File 삭제 (MultipartFile -> File 전환 하며 로컬에 파일 생성됨)
//        return uploadImageUrl;
//    }
    // 업로드하기
    private String putS3(File uploadFile, String fileName){
        amazonS3Client.putObject(new PutObjectRequest(bucket, fileName, uploadFile));
        return amazonS3Client.getUrl(bucket, fileName).toString();
    }
    private String putS3(String fileName,InputStream uploadFile, ObjectMetadata metadata) {
        log.info("Uploading to S3 bucket: {}, file: {}", bucket, fileName);
        amazonS3Client.putObject(new PutObjectRequest(bucket, fileName, uploadFile,metadata));
        return amazonS3Client.getUrl(bucket, fileName).toString();
    }
    // 이미지 지우기
    private void removeNewFile(File targetFile) {
        if (targetFile.delete()) {
            log.info("File delete success");
            return;
        }
        log.info("File delete fail");
    }
    private Optional<File> convert(MultipartFile file) throws  IOException {
        File convertFile = new File(file.getOriginalFilename()); // 업로드한 파일의 이름
        if(convertFile.createNewFile()) {
            try (FileOutputStream fos = new FileOutputStream(convertFile)) {
                fos.write(file.getBytes());
            }
            return Optional.of(convertFile);
        }
        return Optional.empty();
    }

    // MultipartFile이 지원하는 이미지 파일 형식인지 검증
    public boolean isFileImage(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1);

        log.info("extension : {}", extension);

        return AllowedImageFileExtensions.contains(extension);
    }


    private String getFileExtension(String[] base64Components) {
        String filePrefix = base64Components[0].split(";")[0].split(":")[1];
        return switch (filePrefix) {
            case "image/jpeg" -> ".jpeg";
            case "image/png" -> ".png";
            case "image/jpg" -> ".jpg";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "image/svg+xml" -> ".svg";
            case "image/bmp" -> ".bmp";
            case "image/tif" -> ".tif";
            case "image/tiff" -> ".tiff";
            case "image/heic" -> ".heic";
            case "application/pdf" -> ".pdf";
            case "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx";
            case "application/vnd.hwp" -> ".hwp";
            case "text/plain" -> ".txt";
            default -> {
                log.info("Unsupported file prefix: " + filePrefix);
                yield ".bin"; // 기본 확장자
            }
        };
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
