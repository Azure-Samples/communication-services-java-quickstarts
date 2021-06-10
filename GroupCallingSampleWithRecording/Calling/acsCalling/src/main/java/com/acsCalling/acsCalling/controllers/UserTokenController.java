package com.acsCalling.acsCalling.controllers;

import com.azure.communication.identity.CommunicationIdentityClient;
import com.azure.communication.identity.CommunicationIdentityClientBuilder;
import com.azure.communication.identity.models.CommunicationTokenScope;
import com.azure.communication.identity.models.CommunicationUserIdentifierAndToken;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import org.json.simple.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class UserTokenController {

    private CommunicationIdentityClient communicationIdentityClient = null;
    UserTokenController(){
        String connectionString = "endpoint=https://acstelephonyportaltesting.communication.azure.com/;accesskey=tXG1rbtQsQxj+W+wop/xr3GU4N7NG0WTWzyvDLi24KoYyLPlZwcdiVpSx5gq6ow5uKr6r/HewimEyiTkgtWyYg==";
        NettyAsyncHttpClientBuilder httpClientBuilder = new NettyAsyncHttpClientBuilder();
        CommunicationIdentityClientBuilder builder = new CommunicationIdentityClientBuilder().httpClient(httpClientBuilder.build())
                .connectionString(connectionString);
        communicationIdentityClient = builder.buildClient();
    }

    @GetMapping("/token")
    public JSONObject usertoken(){
        List<CommunicationTokenScope> list = new ArrayList<>();
        list.add(CommunicationTokenScope.VOIP);
        CommunicationUserIdentifierAndToken data = communicationIdentityClient.createUserAndToken(list);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("communicationUserId", data.getUser().getId());

        JSONObject obj = new JSONObject();
        try {
            obj.put("user", jsonObject);
            obj.put("token", data.getUserToken().getToken());
        } catch(Exception e) {
        }
        return obj;
    }
}
