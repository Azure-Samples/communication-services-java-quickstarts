// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.acsrecording.api.Controller;


import com.acsrecording.api.ConfigurationManager;
import com.azure.communication.callautomation.CallAutomationClientBuilder ;
import com.azure.communication.callautomation.CallAutomationEventParser;
import com.azure.communication.callautomation.models.CallInvite;
import com.azure.communication.callautomation.models.CreateCallOptions;
import com.azure.communication.callautomation.models.RecordingState;
import com.azure.communication.callautomation.models.ServerCallLocator;
import com.azure.core.http.HttpHeader;
import com.azure.communication.callautomation.models.RecordingStateResult;
import com.azure.communication.callautomation.models.StartRecordingOptions;
import com.azure.communication.callautomation.models.events.CallAutomationEventBase;
import com.azure.communication.callautomation.models.events.CallConnected;
import com.azure.communication.common.PhoneNumberIdentifier;
import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.cosmos.implementation.Strings;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
public class RecordingsController  {
    //private static final Object CallConnected = null;
    private static String _serverCallId = "";
    private static String _callConnectionId = "";
    private static String _recordingId = "";
    private static String _contentLocation = "";
    private static String _deleteLocation = "";
    Map<String,String> recordingDataMap;    
    String ACSAcquiredPhoneNumber; 
    String targetPhoneNumber;  
    String BaseUri;
    Logger logger;
    String recordingFileFormat;
    private final com.azure.communication.callautomation.CallAutomationClient callAutomationClient;

    RecordingsController() {
        ConfigurationManager configurationManager = ConfigurationManager.getInstance();
        String connectionString = configurationManager.getAppSettings("Connectionstring");
        ACSAcquiredPhoneNumber= configurationManager.getAppSettings("ACSAcquiredPhoneNumber");
        BaseUri = configurationManager.getAppSettings("BaseUri");
        targetPhoneNumber= configurationManager.getAppSettings("targetPhoneNumber");

        callAutomationClient  = new CallAutomationClientBuilder().connectionString(connectionString).buildClient();
        logger =  Logger.getLogger(RecordingsController.class.getName());
        recordingDataMap = new HashMap<>();
    }

    @GetMapping("/OutboundCall")
    public String OutboundCall() {
        try {            
            var callerId = new PhoneNumberIdentifier(ACSAcquiredPhoneNumber);
            var target = new PhoneNumberIdentifier(targetPhoneNumber);
            var callInvite = new CallInvite(target, callerId);
            var createCallOption = new CreateCallOptions(callInvite, BaseUri + "/api/callbacks");           
         var response = this.callAutomationClient.createCallWithResponse(createCallOption,null);
         _callConnectionId = response.getValue().getCallConnection().getCallProperties().getCallConnectionId();
         logger.log(Level.INFO, "Create call response --> " + _callConnectionId);
         return _callConnectionId;
        }
        catch (Exception e) {
        e.printStackTrace();
        return null;
        }

    }
    @GetMapping("/StartRecording")
    public String startRecording(String serverCallId) {        
        try {
            _serverCallId = Strings.isNullOrEmpty(serverCallId) ? callAutomationClient.getCallConnection(_callConnectionId).getCallProperties().getServerCallId():serverCallId;
            StartRecordingOptions recordingOptions = new StartRecordingOptions(new ServerCallLocator(_serverCallId));
            Response<RecordingStateResult> response = this.callAutomationClient.getCallRecording().startWithResponse(recordingOptions, null);
            logger.log(Level.INFO, "Start Recording response --> " + getResponse(response) + "\n recording ID: " + response.getValue().getRecordingId());
            _recordingId = response.getValue().getRecordingId();
            return _recordingId;

        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @GetMapping("/PauseRecording")
    public void pauseRecording(String  recordingId){
        _recordingId = Strings.isNullOrEmpty(recordingId)? _recordingId : recordingId;
            Response<Void> response = this.callAutomationClient.getCallRecording().pauseWithResponse(_recordingId, null);
            logger.log(Level.INFO, "Pause Recording response --> " + getResponse(response));           
        
    }

    @GetMapping("/ResumeRecording")
    public void resumeRecording(String serverCallId, String  recordingId){
        _recordingId = Strings.isNullOrEmpty(recordingId)? _recordingId : recordingId;
            Response<Void> response = this.callAutomationClient.getCallRecording().resumeWithResponse(_recordingId, null);
            logger.log(Level.INFO, "Resume Recording response --> " + getResponse(response));
        
    }

    @GetMapping("/StopRecording")
    public void stopRecording(String  recordingId){      
            _recordingId = Strings.isNullOrEmpty(recordingId)? _recordingId : recordingId;
            Response<Void> response = this.callAutomationClient.getCallRecording().stopWithResponse(_recordingId, null);
            logger.log(Level.INFO, "Stop Recording response --> " + getResponse(response));
        
    }

    @GetMapping("/GetRecordingState")
    public RecordingState getRecordingState(String recordingId) {
        try {
            _recordingId = Strings.isNullOrEmpty(recordingId)? _recordingId : recordingId;
            RecordingStateResult recordingStateResult = this.callAutomationClient.getCallRecording().getState(_recordingId);
            logger.log(Level.INFO, "Recording State --> " + recordingStateResult.getRecordingState().toString());
            return recordingStateResult.getRecordingState();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @GetMapping("DownloadRecording")
    public void DownloadRecording() {
        try {
            var callRecording = callAutomationClient.getCallRecording();
            OutputStream out = new FileOutputStream("Recording_File.wav");
            callRecording.downloadTo( _contentLocation , out);            
            }
        catch (Exception e) {
            e.printStackTrace();
           
        }
    } 
    @DeleteMapping("DeleteRecording")
    public void DeleteRecording() {
        try {
             callAutomationClient.getCallRecording().delete(_deleteLocation);                       
            }
        catch (Exception e) {
            e.printStackTrace();
           
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

                    _contentLocation =  recordingChunk.getContentLocation();
                    _deleteLocation =  recordingChunk.getDeleteLocation();  
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

    @PostMapping(value = "/api/callbacks")
    public void Callbacks(@RequestBody String requestBody){
        List<CallAutomationEventBase> acsEvents = CallAutomationEventParser.parseEvents(requestBody);     
        for (CallAutomationEventBase acsEvent : acsEvents) {
            if (acsEvent instanceof CallConnected) {
                CallConnected event = (CallConnected) acsEvent;
                _serverCallId = event.getServerCallId();
                logger.log(Level.INFO, "Server Call Id: -- > " + _serverCallId); 
                                 
            }
        }        
      
    }          
    
}
