// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.acsrecording.api.Controller;

import com.acsrecording.api.ConfigurationManager;
import com.azure.communication.callingserver.CallingServerClientBuilder;
import com.azure.communication.callingserver.models.CallRecordingProperties;
import com.azure.communication.callingserver.models.CallRecordingState;
import com.azure.core.http.HttpHeader;
import com.azure.communication.callingserver.models.StartCallRecordingResult;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.google.common.base.Strings;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.SystemEventNames;
import com.azure.messaging.eventgrid.systemevents.AcsRecordingChunkInfoProperties;
import com.azure.messaging.eventgrid.systemevents.AcsRecordingFileStatusUpdatedEventData;
import com.azure.messaging.eventgrid.systemevents.SubscriptionValidationEventData;
import com.azure.messaging.eventgrid.systemevents.SubscriptionValidationResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
    Map<String,String> recordingDataMap;
    String container;
    String blobStorageConnectionString;
    String recordingStateCallbackUrl;
    Logger logger;
    private final com.azure.communication.callingserver.CallingServerClient callingServerClient;

    CallRecordingController() {
        ConfigurationManager configurationManager = ConfigurationManager.getInstance();
        String connectionString = configurationManager.getAppSettings("Connectionstring");
        container = configurationManager.getAppSettings("ContainerName");
        recordingStateCallbackUrl = configurationManager.getAppSettings("CallbackUri");
        blobStorageConnectionString = configurationManager.getAppSettings("BlobStorageConnectionString");

        NettyAsyncHttpClientBuilder httpClientBuilder = new NettyAsyncHttpClientBuilder();
        CallingServerClientBuilder builder = new CallingServerClientBuilder().httpClient(httpClientBuilder.build())
                .connectionString(connectionString);
        callingServerClient = builder.buildClient();
        logger =  Logger.getLogger(CallRecordingController.class.getName());
        recordingDataMap = new HashMap<>();
    }

    @GetMapping("/startRecording")
    public StartCallRecordingResult startRecording(String serverCallId) {
        URI recordingStateCallbackUri;
        try {
            recordingStateCallbackUri = new URI(recordingStateCallbackUrl);
            Response<StartCallRecordingResult> response = this.callingServerClient.initializeServerCall(serverCallId).startRecordingWithResponse(String.valueOf(recordingStateCallbackUri),null);
            var output = response.getValue();

            logger.log(Level.INFO, "Start Recording response --> " + getResponse(response) + "\n recording ID: " + response.getValue().getRecordingId());
            if(!recordingDataMap.containsKey(serverCallId)){
                recordingDataMap.put(serverCallId, "");
            }
            recordingDataMap.replace(serverCallId, output.getRecordingId());

            return output;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return  null;
        }
    }

    @GetMapping("/pauseRecording")
    public void pauseRecording(String serverCallId, String  recordingId){
        if(!Strings.isNullOrEmpty(serverCallId)){
            if (Strings.isNullOrEmpty(recordingId))
            {
                recordingId = recordingDataMap.get(serverCallId);
            }
            else
            {
                if (!recordingDataMap.containsKey(serverCallId))
                {
                    recordingDataMap.put(serverCallId, recordingId);
                }
            }

            Response<Void> response = this.callingServerClient.initializeServerCall(serverCallId).pauseRecordingWithResponse(recordingId, null);
            logger.log(Level.INFO, "Pause Recording response --> " + getResponse(response));
        }
    }

    @GetMapping("/resumeRecording")
    public void resumeRecording(String serverCallId, String  recordingId){
        if(!Strings.isNullOrEmpty(serverCallId)){
            if (Strings.isNullOrEmpty(recordingId))
            {
                recordingId = recordingDataMap.get(serverCallId);
            }
            else
            {
                if (!recordingDataMap.containsKey(serverCallId))
                {
                    recordingDataMap.put(serverCallId, recordingId);
                }
            }

            Response<Void> response = this.callingServerClient.initializeServerCall(serverCallId).resumeRecordingWithResponse(recordingId, null);
            logger.log(Level.INFO, "Resume Recording response --> " + getResponse(response));
        }
    }

    @GetMapping("/stopRecording")
    public void stopRecording(String serverCallId, String  recordingId){
        if(!Strings.isNullOrEmpty(serverCallId)){
            if (Strings.isNullOrEmpty(recordingId))
            {
                recordingId = recordingDataMap.get(serverCallId);
            }
            else
            {
                if (!recordingDataMap.containsKey(serverCallId))
                {
                    recordingDataMap.put(serverCallId, recordingId);
                }
            }

            Response<Void> response = this.callingServerClient.initializeServerCall(serverCallId).stopRecordingWithResponse(recordingId, null);
            logger.log(Level.INFO, "Stop Recording response --> " + getResponse(response));

            recordingDataMap.remove(serverCallId);
        }
    }

    @GetMapping("/getRecordingState")
    public CallRecordingState getRecordingState(String serverCallId, String recordingId) {
        try {
            if (Strings.isNullOrEmpty(recordingId))
            {
                recordingId = recordingDataMap.get(serverCallId);
            }
            else
            {
                if (!recordingDataMap.containsKey(serverCallId))
                {
                    recordingDataMap.put(serverCallId, recordingId);
                }
            }

            CallRecordingProperties recordingStateResult = this.callingServerClient.initializeServerCall(serverCallId).getRecordingState(recordingId);
            logger.log(Level.INFO, "Recording State --> " + recordingStateResult.getRecordingState().toString());
            
            return recordingStateResult.getRecordingState();
        }
        catch (Exception e) {
            e.printStackTrace();
            return  null;
        }
    }

    @PostMapping(value = "/getRecordingFile", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> getRecordingFile (@RequestBody String eventGridEventJsonData){
        
        logger.log(Level.INFO,  "Entered getRecordingFile API");

        List<EventGridEvent> eventGridEvents = EventGridEvent.fromString(eventGridEventJsonData);

        if(eventGridEvents.stream().count() > 0)
        {
            EventGridEvent eventGridEvent = eventGridEvents.get(0);
            logger.log(Level.INFO,  "Event type is --> " + eventGridEvent.getEventType());

            BinaryData eventData = eventGridEvent.getData();
            logger.log(Level.INFO, "SubscriptionValidationEvent response --> \n" + eventData.toString());

            if (eventGridEvent.getEventType().equals(SystemEventNames.EVENT_GRID_SUBSCRIPTION_VALIDATION))
            {
                try {
                    SubscriptionValidationEventData subscriptionValidationEvent = eventData.toObject(SubscriptionValidationEventData.class);
                    SubscriptionValidationResponse responseData = new SubscriptionValidationResponse();
                    responseData.setValidationResponse(subscriptionValidationEvent.getValidationCode());

                    return new ResponseEntity<>(responseData, HttpStatus.OK);
                } catch (Exception e){
                    e.printStackTrace();
                    return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }

            if(eventGridEvent.getEventType().equals(SystemEventNames.COMMUNICATION_RECORDING_FILE_STATUS_UPDATED)){
                try {
                    AcsRecordingFileStatusUpdatedEventData acsRecordingFileStatusUpdatedEventData =  eventData.toObject(AcsRecordingFileStatusUpdatedEventData.class);
                    AcsRecordingChunkInfoProperties recordingChunk = acsRecordingFileStatusUpdatedEventData
                                                    .getRecordingStorageInfo()
                                                    .getRecordingChunks().get(0);

                    logger.log(Level.INFO, "Processing recording file --> \n");
                    
                    processFile(
                        recordingChunk.getContentLocation(),
                        recordingChunk.getDocumentId(),
                        "mp4",
                        "recording"
                    );
                    
                    logger.log(Level.INFO, "Processing metadata file --> \n");
                    
                    processFile(
                        recordingChunk.getMetadataLocation(),
                        recordingChunk.getDocumentId(),
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
                return new ResponseEntity<>(eventGridEvent.getEventType() + " is not handled.", HttpStatus.BAD_REQUEST);
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
        logger.log(Level.INFO, String.format("Download media response --> %s", getResponse(downloadResponse)));

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

    private String getResponse(Response<?> response)
    {
        StringBuilder responseString;
        responseString = new StringBuilder("StatusCode: " + response.getStatusCode() + "\nHeaders: { ");

        for (HttpHeader header : response.getHeaders()) {
            responseString.append(header.getName()).append(": ").append(header.getValue()).append(", ");
        }
        responseString.append("} ");
        return responseString.toString();
    }
}
