package org.example.service;


import org.example.model.PollyRequest;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.OutputFormat;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.UUID;

public class PollyService {

 private final PollyClient pollyClient;
 private final S3Client s3Client;
 private final S3Presigner s3Presigner;
 private final String bucketName;

    public PollyService(String bucketName, Region region) {
       this.pollyClient = PollyClient.builder().region(region).build();
        this.s3Client = S3Client.builder().region(region).build();
        this.s3Presigner = S3Presigner.builder().region(region).build();
        this.bucketName = bucketName;
    }

    private InputStream synthesizeSpeech(PollyRequest pollyRequest){
       SynthesizeSpeechRequest speechRequest = SynthesizeSpeechRequest.builder()
               .engine(pollyRequest.getEngineId())
               .languageCode(pollyRequest.getLanguageId())
               .voiceId(pollyRequest.getVoiceId())
               .text( pollyRequest.getText())
               .outputFormat(OutputFormat.MP3)
               .build();

       ResponseInputStream<SynthesizeSpeechResponse> synthRes = pollyClient.synthesizeSpeech(speechRequest);
       return synthRes;
    }

    public String synthesizeAndUploadToS3(PollyRequest pollyRequest) throws IOException {
        String audioKey = UUID.randomUUID() + ".mp3";

        try(InputStream audioStream = synthesizeSpeech(pollyRequest)){
            uploadToS3(audioStream, audioKey);
            return generatePresignedUrl(audioKey);
        }
    }

    /*Usar se necessario*/
    private String truncateAndAddHyphen(String pollyText) {
        String hyphen = pollyText.replaceAll("\\s+", "-");
        return hyphen.length() > 20 ? hyphen.substring(0, 20) : hyphen;
    }

    private void uploadToS3(InputStream audioStream, String audioKey) throws IOException {
        Path tempFilePath = Paths.get("/tmp/" + audioKey);
        Files.copy(audioStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key("audios/" + audioKey)
                .build();

        s3Client.putObject(putObjectRequest, tempFilePath);
        Files.delete(tempFilePath);
    }

    private String generatePresignedUrl(String audioKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key("audios/" + audioKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}
