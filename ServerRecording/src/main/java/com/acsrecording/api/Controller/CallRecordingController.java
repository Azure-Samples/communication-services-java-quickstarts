package com.acsrecording.api.Controller;

import com.acsrecording.api.ConfigurationManager;
import com.acsrecording.api.RecordingChunk;
import com.acsrecording.api.RecordingStorage;
import com.azure.communication.callingserver.CallingServerClientBuilder;
import com.azure.communication.callingserver.models.CallRecordingProperties;
import com.azure.core.http.HttpHeader;
import com.azure.core.http.HttpRange;
import com.azure.communication.callingserver.models.StartCallRecordingResult;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobContainerItem;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.microsoft.azure.eventgrid.models.EventGridEvent;
import com.microsoft.azure.eventgrid.models.SubscriptionValidationEventData;
import com.microsoft.azure.eventgrid.models.SubscriptionValidationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
public class CallRecordingController  {
    Map<String,String> recordingData = new HashMap<>();

    String container = "";
    String blobStorageConnectionString = "";
    String recordingStateCallbackUrl = "";
    Logger logger = null;
    private com.azure.communication.callingserver.CallingServerClient callingServerClient = null;
    CallRecordingController() {
        ConfigurationManager configurationManager = ConfigurationManager.GetInstance();
        String connectionString = configurationManager.GetAppSettings("Connectionstring");
        container = configurationManager.GetAppSettings("ContainerName");
        recordingStateCallbackUrl = configurationManager.GetAppSettings("CallbackUri");
        blobStorageConnectionString = configurationManager.GetAppSettings("BlobStorageConnectionString");

        NettyAsyncHttpClientBuilder httpClientBuilder = new NettyAsyncHttpClientBuilder();
        CallingServerClientBuilder builder = new CallingServerClientBuilder().httpClient(httpClientBuilder.build())
                .connectionString(connectionString);
        callingServerClient = builder.buildClient();
        logger =  Logger.getLogger(CallRecordingController.class.getName());
    }


    @GetMapping("/getrecordingstatus")
    public CallRecordingProperties  getRecordingState(String serverCallId, String recordingId) {
        CallRecordingProperties recordingStateResult = this.callingServerClient.initializeServerCall(serverCallId).getRecordingState(recordingId);
        logger.log(Level.INFO, "Recording State --> " + recordingStateResult.getRecordingState().toString());
        return recordingStateResult;
    }

    @GetMapping("/startRecording")
    public StartCallRecordingResult startRecordingState(String serverCallId) {
        URI recordingStateCallbackUri = null;
        try {
            recordingStateCallbackUri = new URI(recordingStateCallbackUrl);
            Response<StartCallRecordingResult> response = this.callingServerClient.initializeServerCall(serverCallId).startRecordingWithResponse(String.valueOf(recordingStateCallbackUri),null);
            var output = response.getValue();

            logger.log(Level.INFO, "Start Recording response --> " + GetResponse(response) + "\n recording ID: " + response.getValue().getRecordingId());
            if(!recordingData.containsKey(serverCallId)){
                recordingData.put(serverCallId, "");
            }
            recordingData.replace(serverCallId, output.getRecordingId());
            return output;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return  null;
        }
    }

    @GetMapping("/pauseRecording")
    public void pauseRecordingState(String serverCallId, String  recordingId){
        if(!Strings.isNullOrEmpty(serverCallId)){
            if (Strings.isNullOrEmpty(recordingId))
            {
                recordingId = recordingData.get(serverCallId);
            }
            else
            {
                if (!recordingData.containsKey(serverCallId))
                {
                    recordingData.put(serverCallId, recordingId);
                }
            }
            Response<Void> response = this.callingServerClient.initializeServerCall(serverCallId).pauseRecordingWithResponse(recordingId, null);
            logger.log(Level.INFO, "Pause Recording response --> " + GetResponse(response));
        }
    }
    @GetMapping("/resumeRecording")
    public void resumerecording(String serverCallId, String  recordingId){
        if(!Strings.isNullOrEmpty(serverCallId)){
            if (Strings.isNullOrEmpty(recordingId))
            {
                recordingId = recordingData.get(serverCallId);
            }
            else
            {
                if (!recordingData.containsKey(serverCallId))
                {
                    recordingData.put(serverCallId, recordingId);
                }
            }
            Response<Void> response = this.callingServerClient.initializeServerCall(serverCallId).resumeRecordingWithResponse(serverCallId, null);
            logger.log(Level.INFO, "Resume Recording response --> " + GetResponse(response));
        }
    }
    @GetMapping("/stopRecording")
    public void stoprecording(String serverCallId, String  recordingId){
        if(!Strings.isNullOrEmpty(serverCallId)){
            if (Strings.isNullOrEmpty(recordingId))
            {
                recordingId = recordingData.get(serverCallId);
            }
            else
            {
                if (!recordingData.containsKey(serverCallId))
                {
                    recordingData.put(serverCallId, recordingId);
                }
            }
            Response<Void> response = this.callingServerClient.initializeServerCall(serverCallId).stopRecordingWithResponse(recordingId, null);
            logger.log(Level.INFO, "Stop Recording response --> " + GetResponse(response));
            if (recordingData.containsKey(serverCallId))
            {
                recordingData.remove(serverCallId);
            }
        }
    }

    @PostMapping(value = "/getRecordingFile", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> getRecordingFile (@RequestBody List<EventGridEvent> obj){
        EventGridEvent event = null;
        SubscriptionValidationResponse responseData = null;
        logger.log(Level.INFO,  "Entered getRecordingFile API");
        if(obj.stream().count() > 0){
            event = obj.get(0);
            logger.log(Level.INFO,  "Event type is --> " + event.eventType());

            if (event.eventType().equals("Microsoft.EventGrid.SubscriptionValidationEvent")) {
                String json = new Gson().toJson(event.data());
                ObjectMapper mapper = new ObjectMapper();
                mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                logger.log(Level.INFO, "SubscriptionValidationEvent response --> \n" + json);
                try {
                    SubscriptionValidationEventData subscriptionValidationEvent = mapper.readValue(json, SubscriptionValidationEventData.class);
                    responseData = new SubscriptionValidationResponse();
                    responseData.withValidationResponse(subscriptionValidationEvent.validationCode());
                    return new ResponseEntity<>(responseData, HttpStatus.OK);
                } catch (Exception e){
                    e.printStackTrace();
                    return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }

            if(event.eventType().equals("Microsoft.Communication.RecordingFileStatusUpdated")){
                String json = new Gson().toJson(event.data());
                ObjectMapper mapper = new ObjectMapper();
                mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

                try {
                    logger.log(Level.INFO, "RecordingFileStatusUpdated JSON response --> \n" + json);
                    RecordingStorage recordingData = mapper.readValue(json, RecordingStorage.class);
                    RecordingChunk recordingChunk = recordingData.recordingStorageInfo.recordingChunks.get(0);

                    logger.log(Level.INFO, "Processing recording file --> \n");
                    
                    processFile(
                        recordingChunk.contentLocation,
                        recordingChunk.documentId,
                        "mp4",
                        "recording"
                    );
                    
                    logger.log(Level.INFO, "Processing metadata file --> \n");
                    
                    processFile(
                        recordingChunk.metadataLocation,
                        recordingChunk.documentId,
                        "json",
                        "metadata"
                    );

                    return new ResponseEntity<>(true, HttpStatus.OK);
                } catch (Exception e){
                    e.printStackTrace();
                    logger.log(Level.SEVERE, e.getMessage());
                    return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
            else{
                return new ResponseEntity<>(event.eventType() + " is not handled.", HttpStatus.BAD_REQUEST);
            }
        }

        return new ResponseEntity<>("Event count is not available.", HttpStatus.BAD_REQUEST);
    }

    private void processFile(String url, String documentId, String fileType, String downloadType) throws Exception
    {
        logger.log(Level.INFO, String.format("Start downloading %s file -- >", downloadType));
        logger.log(Level.INFO, String.format("%s url -- > %s", downloadType, url));

        String fileName = String.format("%s.%s", documentId, fileType);
        String filePath = String.format(".\\%s", fileName);
        Path path = Paths.get(filePath);
        logger.log(Level.INFO, String.format("Local file Path url -- > %s", filePath));

        var downloadResponse = callingServerClient.downloadToWithResponse(url, path, null, true, null);
        logger.log(Level.INFO, String.format("Download media response --> %s", GetResponse(downloadResponse)));

        logger.log(Level.INFO, String.format("Uploading %s file to blob -- >", downloadType));
        uploadFileToStorage(fileName, filePath);
    }

    private void uploadFileToStorage(String fileName, String filePath) throws Exception
    {
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(blobStorageConnectionString).buildClient();
        BlobContainerClient bClient = blobServiceClient.getBlobContainerClient(container);
        BlobClient blobClient = bClient.getBlobClient(fileName);

        blobClient.uploadFromFile(filePath);
        logger.log(Level.INFO, "File upload to Azure successful");

        logger.log(Level.INFO, "Deleting temporary file");
        Files.delete(Paths.get(filePath));
    }

    private String GetResponse(Response<?> response)
    {
        String responseString = null;
        responseString = "StatusCode: " + response.getStatusCode() + "\nHeaders: { ";
        Iterator<HttpHeader> headers = response.getHeaders().iterator();

        while(headers.hasNext())
        {
            HttpHeader header = headers.next();
            responseString += header.getName()+ ": " + header.getValue().toString() + ", ";
        }
        responseString += "} ";
        return responseString;
    }
}
