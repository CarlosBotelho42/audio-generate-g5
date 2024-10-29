package org.example.service;


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

public class PollyService {

 private final PollyClient pollyClient;
 private final S3Client s3Client;
 private final String bucketName;

    public PollyService(String bucketName, Region region) {
       this.pollyClient = PollyClient.builder().region(region).build();
        this.s3Client = S3Client.builder().region(region).build();
        this.bucketName = bucketName;
    }

    //TODO Adicionar o tipo escolhido de engine e tambem a linguagem(ficar parametrizavel)
    private InputStream synthesizeSpeech(String text, String voiceId){
       SynthesizeSpeechRequest speechRequest = SynthesizeSpeechRequest.builder()
               .text(text)
               .voiceId(voiceId)
               .outputFormat(OutputFormat.MP3)
               .build();

       ResponseInputStream<SynthesizeSpeechResponse> synthRes = pollyClient.synthesizeSpeech(speechRequest);
       return synthRes;
    }

    public String synthesizeAndUploadToS3(String text, String voiceId) throws IOException {
        String audioKey = truncateAndAddIfens(text) + ".mp3";

       try(InputStream audioStream = synthesizeSpeech(text, voiceId)){
          uploadToS3(audioStream, audioKey);
          return generateS3Url(audioKey);
       }
    }

    private String truncateAndAddIfens(String input) {
        String hyphen = input.replaceAll("\\s+", "-");
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
