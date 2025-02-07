package friendy.community.infra.storage.s3.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import friendy.community.global.exception.ErrorCode;
import friendy.community.global.exception.FriendyException;
import friendy.community.infra.storage.s3.exception.S3exception;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3service {
    private final AmazonS3 s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;


    private final S3exception s3exception;

    public String upload(MultipartFile multipartFile, String dirName) {

        s3exception.validateFile(multipartFile);

        // (1) MultipartFile을 File로 변환
        File uploadFile = convert(multipartFile);

        // (3)S3에 파일을 업로드. 업로드 완료 여부와 관계 없이 (1)에서 임시 경로에 생성된 파일을 삭제
        try {
             return putS3(uploadFile, generateStoredFileName(multipartFile,dirName));
        } finally {
            removeNewFile(uploadFile);
        }
    }

    private File convert(MultipartFile file) {
        try {
            // 임시 경로에 파일 생성
            File convertFile = new File(System.getProperty("java.io.tmpdir"), file.getOriginalFilename());

            // 파일 생성 실패 시 예외 발생
            if (!convertFile.createNewFile()) {
                throw new FriendyException(ErrorCode.INVALID_FILE, "파일 생성에 실패했습니다.");
            }

            // 파일에 데이터 쓰기
            try (FileOutputStream fos = new FileOutputStream(convertFile)) {
                fos.write(file.getBytes());
            }
            return convertFile;

        } catch (IOException e) {
            throw new FriendyException(ErrorCode.FILE_IO_ERROR, "I/O 오류 발생");
        }
    }


    private String putS3(File uploadFile, String fileName) {
        // S3에 파일을 업로드한다.
        s3Client.putObject(new PutObjectRequest(bucket, fileName, uploadFile));
        // 업로드된 파일의 경로를 가져온다.
        return s3Client.getUrl(bucket, fileName).toString();
    }

    public String generateStoredFileName(MultipartFile multipartFile, String dirName) {
        // MultipartFile에서 원본 파일 이름을 가져오기
        String originalFileName = multipartFile.getOriginalFilename();

        if (originalFileName == null) {
            throw new IllegalArgumentException("파일 이름을 가져올 수 없습니다.");
        }
        // 원본 파일 이름에서 확장자 추출
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        // UUID를 이용해 고유한 파일 이름 생성
        String uuid = UUID.randomUUID().toString();
        // 고유한 파일 이름 반환
        return dirName + "/" + uuid + extension; // 예: 123e4567-e89b-12d3-a456-426614174000.jpg
    }

    public String getFileType(MultipartFile multipartFile) {
        // 파일의 MIME 타입(파일 타입) 확인
        String contentType = multipartFile.getContentType();

        if (contentType == null) {
            throw new IllegalArgumentException("파일 타입을 가져올 수 없습니다.");
        }

        return contentType;  // 예: image/jpeg, application/pdf
    }

    private void removeNewFile(File targetFile) {
        if (targetFile.delete()) {
            log.info("파일이 삭제되었습니다.");
        } else {
            log.info("파일이 삭제되지 못했습니다.");
        }
    }

    public void moveS3Object(String imageUrl, String newDirName) {
        String oldKey = extractFileName(imageUrl);

        // 2. 새 경로로 객체 키 생성
        String fileName = oldKey.substring(oldKey.lastIndexOf("/") + 1); // 파일명만 추출
        String newKey = newDirName + "/" + fileName; // 새로운 경로 생성

        // 3. 객체 복사 (기존 위치 → 새 위치)
        copyObject(bucket, oldKey, bucket, newKey);

    }

    public void copyObject(String sourceBucket, String sourceKey, String destinationBucket, String destinationKey) {
        CopyObjectRequest copyObjectRequest = new CopyObjectRequest(
            sourceBucket, sourceKey,
            destinationBucket, destinationKey);
        s3Client.copyObject(copyObjectRequest);
    }

    public String extractFileName(String imageUrl) {
        try {
            URL url = new URL(imageUrl);  // URL 객체 생성
            String path = url.getPath();  // "/profile/user123.jpg"
            return path.startsWith("/") ? path.substring(1) : path;  // 첫 '/' 제거
        } catch (MalformedURLException e) {  // MalformedURLException 사용
            throw new FriendyException(ErrorCode.INVALID_FILE, "유효한 URL 형식이어야 합니다.");
        }
    }

    public String getContentTypeFromS3(String key) {
        try {
            S3Object object = s3Client.getObject(new GetObjectRequest(bucket, key));
            return object.getObjectMetadata().getContentType();
        } catch (AmazonServiceException e) {
            // 예외 처리 (더 구체적인 예외 처리 가능)
            System.err.println("Error getting object metadata: " + e.getMessage());
            return null;
        }
    }
}

