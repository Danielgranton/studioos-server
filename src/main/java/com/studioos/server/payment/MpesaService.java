package com.studioos.server.payment;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studioos.server.payment.dto.B2cInitiationResult;
import com.studioos.server.payment.dto.MpesaCallbackResult;
import com.studioos.server.payment.dto.StkPushInitiationResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MpesaService {

    private final MpesaProperties mpesaProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String baseUrl() {
        return "sandbox".equalsIgnoreCase(mpesaProperties.getEnvironment())
                ? "https://sandbox.safaricom.co.ke"
                : "https://api.safaricom.co.ke";
    }

    // ─── OAuth ───
    private String getAccessToken() {
        String credentials = mpesaProperties.getConsumerKey() + ":" + mpesaProperties.getConsumerSecret();
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encoded);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        String url = baseUrl() + "/oauth/v1/generate?grant_type=client_credentials";
        JsonNode response = restTemplate.exchange(url, HttpMethod.GET, request, JsonNode.class).getBody();

        if (response == null || !response.has("access_token")) {
            throw new IllegalStateException("Failed to obtain M-Pesa access token");
        }
        return response.get("access_token").asText();
    }

    private String timestamp() {
        return DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
    }

    private String stkPassword(String timestamp) {
        String raw = mpesaProperties.getShortcode() + mpesaProperties.getPasskey() + timestamp;
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    // ─── C2B: STK Push ───
    public StkPushInitiationResult initiateStkPush(String phoneNumber, int amount, String transactionId) {
        String token = getAccessToken();
        String ts = timestamp();

        Map<String, Object> body = new HashMap<>();
        body.put("BusinessShortCode", mpesaProperties.getShortcode());
        body.put("Password", stkPassword(ts));
        body.put("Timestamp", ts);
        body.put("TransactionType", "CustomerPayBillOnline");
        body.put("Amount", amount);
        body.put("PartyA", phoneNumber);
        body.put("PartyB", mpesaProperties.getShortcode());
        body.put("PhoneNumber", phoneNumber);
        body.put("CallBackURL", mpesaProperties.getCallbackUrl());
        body.put("AccountReference", transactionId);
        body.put("TransactionDesc", "StudioOS booking payment");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        String url = baseUrl() + "/mpesa/stkpush/v1/processrequest";

        try {
            JsonNode response = restTemplate.postForObject(url, request, JsonNode.class);
            boolean accepted = response != null && "0".equals(response.path("ResponseCode").asText());

            return new StkPushInitiationResult(
                    accepted,
                    response != null ? response.path("MerchantRequestID").asText(null) : null,
                    response != null ? response.path("CheckoutRequestID").asText(null) : null,
                    response != null ? response.path("ResponseDescription").asText(null) : "No response"
            );
        } catch (Exception e) {
            log.error("STK Push failed for transaction {}: {}", transactionId, e.getMessage());
            return new StkPushInitiationResult(false, null, null, e.getMessage());
        }
    }

    // ─── C2B callback parsing ───
    public MpesaCallbackResult parseStkCallback(String rawCallbackJson) {
        try {
            JsonNode root = objectMapper.readTree(rawCallbackJson);
            JsonNode stkCallback = root.path("Body").path("stkCallback");

            int resultCode = stkCallback.path("ResultCode").asInt(-1);
            boolean success = resultCode == 0;
            String checkoutRequestId = stkCallback.path("CheckoutRequestID").asText(null);

            if (!success) {
                return new MpesaCallbackResult(false, null, 0,
                        stkCallback.path("ResultDesc").asText(), checkoutRequestId);
            }

            JsonNode items = stkCallback.path("CallbackMetadata").path("Item");
            String receipt = null;
            int amount = 0;

            for (JsonNode item : items) {
                String name = item.path("Name").asText();
                if ("MpesaReceiptNumber".equals(name)) {
                    receipt = item.path("Value").asText();
                } else if ("Amount".equals(name)) {
                    amount = item.path("Value").asInt();
                }
            }

            return new MpesaCallbackResult(true, receipt, amount, "Success", checkoutRequestId);
        } catch (Exception e) {
            log.error("Failed to parse STK callback: {}", e.getMessage());
            return new MpesaCallbackResult(false, null, 0, "Parse error: " + e.getMessage(), null);
        }
    }

    // ─── B2C: withdrawals ───
    public B2cInitiationResult initiateB2cPayout(String phoneNumber, int amount, String withdrawalId) {
        if (mpesaProperties.getInitiatorName() == null || mpesaProperties.getInitiatorName().isBlank()
                || mpesaProperties.getSecurityCredential() == null || mpesaProperties.getSecurityCredential().isBlank()) {
            throw new IllegalStateException(
                    "B2C is not configured: MPESA_INITIATOR_NAME and MPESA_SECURITY_CREDENTIAL must be set");
        }

        String token = getAccessToken();

        Map<String, Object> body = new HashMap<>();
        body.put("InitiatorName", mpesaProperties.getInitiatorName());
        body.put("SecurityCredential", mpesaProperties.getSecurityCredential());
        body.put("CommandID", "BusinessPayment");
        body.put("Amount", amount);
        body.put("PartyA", mpesaProperties.getShortcode());
        body.put("PartyB", phoneNumber);
        body.put("Remarks", "StudioOS withdrawal");
        body.put("QueueTimeOutURL", mpesaProperties.getTimeoutUrl());
        body.put("ResultURL", mpesaProperties.getCallbackUrl());
        body.put("Occasion", withdrawalId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        String url = baseUrl() + "/mpesa/b2c/v1/paymentrequest";

        try {
            JsonNode response = restTemplate.postForObject(url, request, JsonNode.class);
            boolean accepted = response != null && "0".equals(response.path("ResponseCode").asText());

            return new B2cInitiationResult(
                    accepted,
                    response != null ? response.path("ConversationID").asText(null) : null,
                    response != null ? response.path("OriginatorConversationID").asText(null) : null,
                    response != null ? response.path("ResponseDescription").asText(null) : "No response"
            );
        } catch (Exception e) {
            log.error("B2C payout failed for withdrawal {}: {}", withdrawalId, e.getMessage());
            return new B2cInitiationResult(false, null, null, e.getMessage());
        }
    }

    // ─── B2C callback parsing ───
    public MpesaCallbackResult parseB2cCallback(String rawCallbackJson) {
        try {
            JsonNode root = objectMapper.readTree(rawCallbackJson);
            JsonNode result = root.path("Result");

            int resultCode = result.path("ResultCode").asInt(-1);
            boolean success = resultCode == 0;
            String occasion = result.path("Occasion").asText(null);

            if (!success) {
                return new MpesaCallbackResult(false, null, 0,
                        result.path("ResultDesc").asText(), occasion);
            }

            JsonNode items = result.path("ResultParameters").path("ResultParameter");
            String receipt = null;
            int amount = 0;

            for (JsonNode item : items) {
                String key = item.path("Key").asText();
                if ("TransactionReceipt".equals(key)) {
                    receipt = item.path("Value").asText();
                } else if ("TransactionAmount".equals(key)) {
                    amount = item.path("Value").asInt();
                }
            }

            return new MpesaCallbackResult(true, receipt, amount, "Success", occasion);
        } catch (Exception e) {
            log.error("Failed to parse B2C callback: {}", e.getMessage());
            return new MpesaCallbackResult(false, null, 0, "Parse error: " + e.getMessage(), null);
        }
    }
}
