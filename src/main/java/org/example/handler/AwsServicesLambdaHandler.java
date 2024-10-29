package org.example.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.example.model.PollyRequest;
import org.example.service.PollyService;
import software.amazon.awssdk.regions.Region;

public class AwsServicesLambdaHandler implements RequestHandler<PollyRequest, String> {

    private static final String BUCKET_NAME = "audios-g5";
    private static final Region REGION = Region.SA_EAST_1;

    private final PollyService pollyService;

    public AwsServicesLambdaHandler(){
        this.pollyService = new PollyService(BUCKET_NAME, REGION);
    }

    @Override
    public String handleRequest(PollyRequest pollyRequest, Context context) {
        try {
            return pollyService.synthesizeAndUploadToS3(pollyRequest.getText(), pollyRequest.getVoiceId());
        } catch (Exception e) {
            context.getLogger().log("Erro ao processar a solicitação: " + e.getMessage() + "\nDetalhes: " + e);
            return "Erro ao processar a solicitação: " + e.getMessage();
        }
    }
}
