package com.example.adsportalbe.services.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.example.adsportalbe.services.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {

    private final AmazonS3 amazonS3;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    public static String getObjectKey(String s3Url) {
        if (s3Url == null || s3Url.isEmpty()) {
            return null;
        }

        Pattern pattern = Pattern.compile("https://[^.]+\\.s3\\.[^.]+\\.amazonaws\\.com/(.*)");
        Matcher matcher = pattern.matcher(s3Url);

        if (matcher.matches()) {
            return matcher.group(1);
        }

        return null;
    }

    @Override
    public String uploadFile(File file) {
        String fileName = generateFileName(file);
        String key = getEnvironmentFolder() + "/" + fileName;

        try {
            amazonS3.putObject(new PutObjectRequest(bucketName, key, file));
            return amazonS3.getUrl(bucketName, key).toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String fileURL) {
        try {
            String key = getObjectKey(fileURL);
            // If the URL logic always returns urls from our bucket, we can just use the
            // bucketName field.
            // However, extracting it from URL serves as a sanity check or support for
            // multi-bucket if needed later,
            // though the original code extracted it. Let's keep it simple and use the
            // configured bucket name for deletion
            // unless we want to strictly follow the provided snippet's logic of extracting
            // bucket from URL.
            // The provided snippet extracted bucket name. I'll stick to using the
            // configured bucket name to be safe against
            // ensuring we only delete from our bucket, but I will extract the key.

            if (key != null) {
                amazonS3.deleteObject(new DeleteObjectRequest(bucketName, key));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file: " + e.getMessage(), e);
        }
    }

    private String generateFileName(File file) {
        return UUID.randomUUID() + "_" + file.getName();
    }

    private String getEnvironmentFolder() {
        return isProd() ? "prod" : "dev";
    }

    private boolean isProd() {
        return "prod".equalsIgnoreCase(activeProfile);
    }
}
