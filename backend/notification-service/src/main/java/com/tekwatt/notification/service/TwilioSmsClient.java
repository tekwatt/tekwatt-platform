package com.tekwatt.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TwilioSmsClient {
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper json;
    private final String accountSid;
    private final String authToken;
    private final String from;

    public TwilioSmsClient(ObjectMapper json,
            @Value("${tekwatt.sms.twilio.account-sid:}") String accountSid,
            @Value("${tekwatt.sms.twilio.auth-token:}") String authToken,
            @Value("${tekwatt.sms.twilio.from:}") String from) {
        this.json=json;this.accountSid=accountSid;this.authToken=authToken;this.from=from;
    }

    public String send(String recipient,String message){
        return send(recipient,message,accountSid,authToken,from);
    }

    public String send(String recipient,String message,String accountSid,String authToken,String from){
        if(!accountSid.matches("AC[0-9a-fA-F]{32}")||authToken.isBlank()||from.isBlank())
            throw new Msg91SmsClient.SmsDeliveryException("Twilio is not configured. Set TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN and TWILIO_FROM on the notification service.");
        String digits=recipient.replaceAll("[^0-9]","");
        String to="+"+(digits.length()==10?"91"+digits:digits);
        if(!to.matches("\\+[1-9][0-9]{9,14}"))
            throw new Msg91SmsClient.SmsDeliveryException("SMS recipient must be a valid mobile number with country code.");
        String form="To="+encode(to)+"&From="+encode(from)+"&Body="+encode(message);
        String authorization=Base64.getEncoder().encodeToString((accountSid+":"+authToken).getBytes(StandardCharsets.UTF_8));
        try{
            HttpRequest request=HttpRequest.newBuilder(URI.create("https://api.twilio.com/2010-04-01/Accounts/"+accountSid+"/Messages.json"))
                    .timeout(Duration.ofSeconds(20)).header("Authorization","Basic "+authorization)
                    .header("Content-Type","application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form)).build();
            HttpResponse<String> response=http.send(request,HttpResponse.BodyHandlers.ofString());
            JsonNode body=json.readTree(response.body());
            if(response.statusCode()<200||response.statusCode()>=300)
                throw new Msg91SmsClient.SmsDeliveryException("Twilio rejected the SMS request: "+body.path("message").asText("check sender credentials and recipient"));
            String sid=body.path("sid").asText("");
            if(sid.isBlank())throw new Msg91SmsClient.SmsDeliveryException("Twilio did not confirm a message ID.");
            return sid;
        }catch(Msg91SmsClient.SmsDeliveryException error){throw error;
        }catch(InterruptedException error){Thread.currentThread().interrupt();throw new Msg91SmsClient.SmsDeliveryException("SMS request was interrupted.");
        }catch(Exception error){throw new Msg91SmsClient.SmsDeliveryException("Twilio could not be reached. Please try again.");}
    }
    private String encode(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
}
