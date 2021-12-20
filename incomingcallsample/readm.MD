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
