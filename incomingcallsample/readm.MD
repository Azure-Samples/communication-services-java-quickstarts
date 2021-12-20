## 1.Start web app locally
```dotnetcli
mvn clean package
java -jar .\target\incomingcallsample-0.0.1-SNAPSHOT.jar
```

## 2.Start ngrok
```dotnetcli
ngrok http 9008
```

## 3.Try to access from brower
### https://ngrok_url/hello

example:
https://19ad-75-155-234-140.ngrok.io/hello

## 4. Register webhook to your ACS resource
```dotnetcli
armclient put "/subscriptions/<subscription id>/resourceGroups/<rg>/providers/Microsoft.Communication/CommunicationServices/<acs name>/providers/Microsoft.EventGrid/eventSubscriptions/IncomingCallEventSub?api-version=2020-06-01" "{'properties':{'destination':{'properties':{'endpointUrl':'https://<ngrok url>/OnIncomingCall'},'endpointType':'WebHook'},'filter':{'includedEventTypes': ['Microsoft.Communication.IncomingCall']}}}" -verbose
```

## Reference
1. https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/eventgrid/azure-messaging-eventgrid/src/samples/java/com/azure/messaging/eventgrid/samples/DeserializeEventsFromString.java
2. https://www.jamessturtevant.com/posts/Validating-Azure-Event-Grid-WebHook-in-Nodejs/