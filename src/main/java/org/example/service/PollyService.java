package org.example.service;


import org.example.model.PollyRequest;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.OutputFormat;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class PollyService {

 private final PollyClient pollyClient;
 private final S3Client s3Client;
 private final String bucketName;

    public PollyService(String bucketName, Region region) {
       this.pollyClient = PollyClient.builder().region(region).build();
        this.s3Client = S3Client.builder().region(region).build();
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
            return generateS3Url(audioKey);
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

    private String generateS3Url(String audioKey) {
       return "https://" + bucketName + ".s3.amazonaws.com/" + audioKey;
   }
}
