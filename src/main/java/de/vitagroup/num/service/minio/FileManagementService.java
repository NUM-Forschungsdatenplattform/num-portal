package de.vitagroup.num.service.minio;

import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@Service
public class FileManagementService {

    public void testMinio() {
        final String numBucket = "num-portal";
            try {
                // Create a minioClient with the MinIO server playground, its access key and secret key.
                MinioClient minioClient =
                        MinioClient.builder()
                                .endpoint("http://127.0.0.1:9000")
                                .credentials("JToNhfY6kDChcnXZjoLA", "vyXxeRrEhFbW7fmvA8CztJrEwQ8B7Nyo00YsvjWK")
                                .build();
                Map<String, String> userMetadata = new HashMap<>();
                userMetadata.put("original-file-name", "mac.jpg");

//                InputStream imageInput = this.getClass().getResourceAsStream("src/test/resources/codex-test-psns/codex_psns_prod_system.csv");
//                ObjectWriteResponse response = minioClient.putObject(PutObjectArgs.builder()
//                        .bucket(numBucket).stream(imageInput, -1, 10485760)
//                                .userMetadata(userMetadata)
//                        .build());
//                ObjectWriteResponse response = minioClient.uploadObject(UploadObjectArgs.builder()
//                                .bucket(numBucket)
//                                .filename("src/test/resources/mac.jpg")
//                                .object("num-mac.jpg")
//                                .userMetadata(userMetadata)
//                        .build());
//                System.out.println("ETAG " + response.etag());
                minioClient.downloadObject(DownloadObjectArgs.builder()
                                .bucket(numBucket)
                                .object("num-mac.jpg")
                                .filename("src/test/resources/downloaded-mac.jpg")
                        .build());

                Iterable<Result<Item>> numObjects = minioClient.listObjects(ListObjectsArgs.builder().bucket(numBucket).build());

                for (Result<Item> result : numObjects) {
                    Item item = result.get();
                    System.out.println(item.lastModified() + ", " + item.size() + ", " + item.objectName() +  "metadata:"  + item.userMetadata());
                }


                // Upload '/home/user/Photos/asiaphotos.zip' as object name 'asiaphotos-2015.zip' to bucket
                // 'asiatrip'.
//                minioClient.uploadObject(
//                        UploadObjectArgs.builder()
//                                .bucket("asiatrip")
//                                .object("asiaphotos-2015.zip")
//                                .filename("/home/user/Photos/asiaphotos.zip")
//                                .build());
//                System.out.println(
//                        "'/home/user/Photos/asiaphotos.zip' is successfully uploaded as "
//                                + "object 'asiaphotos-2015.zip' to bucket 'asiatrip'.");
            } catch (MinioException e) {
                System.out.println("Error occurred: " + e);
                System.out.println("HTTP trace: " + e.httpTrace());
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (InvalidKeyException e) {
                throw new RuntimeException(e);
            }
    }
}
