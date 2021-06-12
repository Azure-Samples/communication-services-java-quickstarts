package com.acsCalling.acsCalling.controllers;

import com.acsCalling.acsCalling.ConfigurationManager;
import com.acsCalling.acsCalling.RecordingChunk;
import com.acsCalling.acsCalling.StorageData;
import com.azure.communication.callingserver.CallingServerClientBuilder;
import com.azure.communication.callingserver.models.CallRecordingStateResponse;
import com.azure.communication.callingserver.models.StartCallRecordingResponse;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobClient;
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
import org.apache.commons.codec.binary.Hex;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
public class CallRecordingController  {
    Map<String,String> recordingData = new HashMap<String, String>();

    String accessKey = "";
    String downloadUri = "";
    String apiVersion = "";
    String container = "";
    String blobStorageConnectionString = "";
    String recordingStateCallbackUrl = "";
    CallRecordingController() {
        ConfigurationManager configurationManager = ConfigurationManager.GetInstance();
        String connectionString = configurationManager.GetAppSettings("Connectionstring");
        accessKey = configurationManager.GetAppSettings("AccessKey");
        downloadUri = configurationManager.GetAppSettings("DownloadUri");
        apiVersion = configurationManager.GetAppSettings("ApiVersion");
        container = configurationManager.GetAppSettings("ContainerName");
        recordingStateCallbackUrl = configurationManager.GetAppSettings("CallbackUri");
        blobStorageConnectionString = configurationManager.GetAppSettings("BlobStorageConnectionString");

        NettyAsyncHttpClientBuilder httpClientBuilder = new NettyAsyncHttpClientBuilder();
        CallingServerClientBuilder builder = new CallingServerClientBuilder().httpClient(httpClientBuilder.build())
                .connectionString(connectionString);
        callingServerClient = builder.buildClient();
    }

    private com.azure.communication.callingserver.CallingServerClient callingServerClient = null;

    @GetMapping("/getrecordingstatus")
    public CallRecordingStateResponse  getRecordingState(String serverCallId, String recordingId) {
        CallRecordingStateResponse recordingStateResult = this.callingServerClient.initializeServerCall(serverCallId).getRecordingState(recordingId);
        return recordingStateResult;
    }

    @GetMapping("/startRecording")
    public StartCallRecordingResponse startRecordingState(String serverCallId) {
        URI recordingStateCallbackUri = null;
        try {
            recordingStateCallbackUri = new URI(recordingStateCallbackUrl);
            Response<StartCallRecordingResponse> response = this.callingServerClient.initializeServerCall(serverCallId).startRecordingWithResponse(String.valueOf(recordingStateCallbackUri),null);
            var output = response.getValue();
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
            this.callingServerClient.initializeServerCall(serverCallId).pauseRecordingWithResponse(recordingId, null);
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
            this.callingServerClient.initializeServerCall(serverCallId).resumeRecordingWithResponse(serverCallId, null);
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
            this.callingServerClient.initializeServerCall(serverCallId).stopRecordingWithResponse(recordingId, null);
            if (recordingData.containsKey(serverCallId))
            {
                recordingData.remove(serverCallId);
            }
        }
    }

    @PostMapping(value = "/GetRecordingFile", consumes = "application/json", produces = "application/json")
    public ResponseEntity GetRecordingFile (@RequestBody List<EventGridEvent> obj){
        EventGridEvent event = null;
        SubscriptionValidationResponse responseData = null;
        if(obj.stream().count() > 0){
          Logger logger =  Logger.getLogger(CallRecordingController.class.getName());
            event = obj.get(0);
            logger.log(Level.INFO,  "Event type is -- > " + event.eventType());

            if (event.eventType().equals("Microsoft.EventGrid.SubscriptionValidationEvent")) {
                String json = new Gson().toJson(event.data());
                ObjectMapper mapper = new ObjectMapper();
                mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
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
           else if(event.eventType().equals("Microsoft.Communication.RecordingFileStatusUpdated")){
                String json = new Gson().toJson(event.data());
                ObjectMapper mapper = new ObjectMapper();
                mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                try {
                    logger.log(Level.INFO, "JSON response ----- \n" + json);
                    StorageData storageData = mapper.readValue(json, StorageData.class);

                    logger.log(Level.INFO, "Start downloading recorded media -- >");

                    var response = Download(storageData.recordingStorageInfo.recordingChunks,  accessKey, downloadUri, apiVersion);
                    String fileName = storageData.recordingStorageInfo.recordingChunks.get(0).documentId + ".mp4";
                    String filePath = ".\\" + fileName;
                    HttpEntity entity = response.getEntity();
                    InputStream inputStream = entity.getContent();

                    logger.log(Level.INFO, "Save downloading media -- >");

                    FileOutputStream fos = new FileOutputStream(filePath);
                    int reader;
                    while((reader = inputStream.read()) != -1) {
                        fos.write(reader);
                    }

                    logger.log(Level.INFO, "Starting to upload recording to BlobStorage into container -- > " + container);
                    String localPath = filePath;

                    // Write text to the file
                    FileWriter writer = new FileWriter(filePath, true);
                    writer.close();

                    BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(blobStorageConnectionString).buildClient();
                    var containers = blobServiceClient.listBlobContainers();

                    for (BlobContainerItem containerItem : containers) {
                       if(containerItem.getName().contentEquals(container)){
                           var bClient = blobServiceClient.getBlobContainerClient(container);
                           BlobClient blobClient = bClient.getBlobClient(filePath);

                           boolean status = false;
                           try{
                               blobClient.uploadFromFile(filePath);
                               status = true;
                               logger.log(Level.INFO, "File upload to Azure successful");
                               return new ResponseEntity<>(status, HttpStatus.OK);
                           }
                           catch (Exception ex){
                               logger.log(Level.WARNING, "File upload to Azure was not successful");
                               ex.printStackTrace();
                               return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                           }
                       }
                    }
                } catch (Exception e){
                    e.printStackTrace();
                    return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
           else{
                return new ResponseEntity<>(event.eventType() + " is not handled.", HttpStatus.BAD_REQUEST);
            }
        }
        return new ResponseEntity<>("Event count is not available.", HttpStatus.BAD_REQUEST);
    }

    public HttpResponse Download(List<RecordingChunk> recordingchunks, String accessKey, String downloadUri, String apiVersion) throws Exception {
        Logger logger = Logger.getLogger(CallRecordingController.class.getName());
        String url = downloadUri + recordingchunks.stream().findFirst().get().documentId + "?apiVersion=" + apiVersion;
        logger.log(Level.INFO, "Download Url -- >  " + url);
        String serializedPayload = "";
        String contentHashed = CreateContentHash(serializedPayload);
        HttpGet request = new HttpGet(URI.create(url));
        List<Header> headers =  AddHmacHeaders(URI.create(url), contentHashed, accessKey);
        HttpClientBuilder  client = HttpClientBuilder.create();
        client.setDefaultHeaders(headers);
        HttpResponse response = client.build().execute(request);
        int responseCode = response.getStatusLine().getStatusCode();

        logger.log(Level.INFO, "Download media api response code -- > " + responseCode);
        logger.log(Level.INFO, "Response Headers as follows -->");
        for(Header header: response.getAllHeaders())
            logger.log(Level.INFO, header.getName() + " -- " + header.getValue());

        return response;
    }

    public String CreateContentHash(String content) throws Exception {
        String secretAccessKey = accessKey;
        byte[] secretKey = secretAccessKey.getBytes();
        SecretKeySpec signingKey = new SecretKeySpec(secretKey, "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(signingKey);
        byte[] bytes = content.getBytes();
        byte[] rawHmac = mac.doFinal(bytes);
        String output = java.util.Base64.getEncoder().encodeToString(rawHmac);
        System.out.println(output);
        return output;
    }


    public static List<Header> AddHmacHeaders(URI requestUri, String contentHash, String accessKey) throws Exception {
        String utcNowString = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss").format(new Date());
        String host = requestUri.getHost();
        String pathAndQuery = requestUri.toURL().getFile(); //+ requestUri.getQuery();
        String stringToSign = "GET" + "\n" + pathAndQuery + "\n" + utcNowString + ";" + host + contentHash;

        byte[] secretKey = DatatypeConverter.parseBase64Binary(accessKey);
        SecretKeySpec signingKey = new SecretKeySpec(secretKey, "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(signingKey);
        byte[] bytes = stringToSign.getBytes();
        byte[] hmac = mac.doFinal(bytes);
        var signature =  Hex.encodeHexString(hmac);
        String authorization = "HMAC-SHA256 SignedHeaders=date;host;x-ms-content-sha256&Signature=" + signature;

        Logger logger = Logger.getLogger(CallRecordingController.class.getName());
        logger.log(Level.INFO, "Request Headers: x-ms-content-sha256 --> " + contentHash);
        logger.log(Level.INFO, "Request Headers: Date --> " + utcNowString);
        logger.log(Level.INFO, "Request Headers: Authorization --> " + authorization);

        Header header1 = new BasicHeader("x-ms-content-sha256", contentHash);
        Header header2 = new BasicHeader("Date", utcNowString);
        Header header3 = new BasicHeader("Authorization", authorization);
        List<Header> headers = Arrays.asList(header1, header2, header3);
        return headers;
    }
}
