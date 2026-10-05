package com.studioos.server.payment;

import java.nio.charset.StandardCharsets;
import java.net.URI;
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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studioos.server.payment.dto.B2cInitiationResult;
import com.studioos.server.payment.dto.MpesaCallbackResult;
import com.studioos.server.payment.dto.StkPushInitiationResult;
import com.studioos.server.payment.dto.StkPushQueryResult;

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
        if (mpesaProperties.getBaseUrl() != null && !mpesaProperties.getBaseUrl().isBlank()) {
            return mpesaProperties.getBaseUrl().trim().replaceAll("/+$", "");
        }
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
        String callbackProblem = callbackUrlProblem(mpesaProperties.getCallbackUrl(), "MPESA_CALLBACK_URL");
        if (callbackProblem != null) {
            log.error("Cannot initiate STK Push for transaction {}: {}", transactionId, callbackProblem);
            return new StkPushInitiationResult(false, null, null, callbackProblem);
        }

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

    public StkPushQueryResult queryStkPush(String checkoutRequestId) {
        try {
            String token = getAccessToken();
            String ts = timestamp();
            Map<String, Object> body = new HashMap<>();
            body.put("BusinessShortCode", mpesaProperties.getShortcode());
            body.put("Password", stkPassword(ts));
            body.put("Timestamp", ts);
            body.put("CheckoutRequestID", checkoutRequestId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);
            JsonNode response = restTemplate.postForObject(
                    baseUrl() + "/mpesa/stkpushquery/v1/query",
                    new HttpEntity<>(body, headers), JsonNode.class);
            if (response == null || !"0".equals(response.path("ResponseCode").asText())) {
                return StkPushQueryResult.unknown("Daraja did not accept the status query");
            }
            if (!response.hasNonNull("ResultCode")) {
                return StkPushQueryResult.pending(response.path("ResultDesc").asText("Payment is still processing"));
            }

            String resultCode = response.path("ResultCode").asText();
            if ("0".equals(resultCode)) {
                return StkPushQueryResult.success(response.path("ResultDesc").asText("Payment confirmed"));
            }
            if ("1".equals(resultCode) || "1032".equals(resultCode) || "1037".equals(resultCode)) {
                return StkPushQueryResult.failed(resultCode,
                        response.path("ResultDesc").asText("Payment was cancelled or timed out"));
            }
            return StkPushQueryResult.pending(response.path("ResultDesc").asText("Payment is not yet confirmed"));
        } catch (HttpStatusCodeException e) {
            String responseBody = e.getResponseBodyAsString();
            try {
                JsonNode error = objectMapper.readTree(responseBody);
                if ("500.001.1001".equals(error.path("errorCode").asText())) {
                    return StkPushQueryResult.failed("CHECKOUT_NOT_FOUND",
                            error.path("errorMessage").asText("Daraja no longer recognizes this checkout"));
                }
            } catch (Exception ignored) {
                // Preserve unknown status for unparseable/transient Daraja errors.
            }
            log.warn("Daraja STK status query failed for checkout {}: {}", checkoutRequestId, e.getMessage());
            return StkPushQueryResult.unknown("Daraja status is temporarily unavailable");
        } catch (Exception e) {
            log.warn("Daraja STK status query failed for checkout {}: {}", checkoutRequestId, e.getMessage());
            return StkPushQueryResult.unknown("Daraja status is temporarily unavailable");
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

        String callbackProblem = callbackUrlProblem(mpesaProperties.getCallbackUrl(), "MPESA_CALLBACK_URL");
        if (callbackProblem == null) {
            callbackProblem = callbackUrlProblem(mpesaProperties.getTimeoutUrl(), "MPESA_TIMEOUT_URL");
        }
        if (callbackProblem != null) {
            log.error("Cannot initiate B2C payout for withdrawal {}: {}", withdrawalId, callbackProblem);
            return new B2cInitiationResult(false, null, null, callbackProblem);
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

    static String callbackUrlProblem(String value, String settingName) {
        if (value == null || value.isBlank()) {
            return settingName + " must be configured as a public HTTPS URL";
        }

        try {
            URI uri = URI.create(value.trim());
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null || uri.getRawUserInfo() != null
                    || uri.getPort() != -1 && uri.getPort() != 443 || isLocalHost(host)) {
                return settingName + " must be a public HTTPS URL reachable by Safaricom; localhost and private-network URLs are not accepted";
            }
        } catch (IllegalArgumentException exception) {
            return settingName + " is not a valid HTTPS URL";
        }
        return null;
    }

    private static boolean isLocalHost(String hostValue) {
        String host = hostValue.toLowerCase();
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        if (host.equals("localhost") || host.endsWith(".localhost") || host.endsWith(".local")
                || host.equals("::1") || host.equals("0:0:0:0:0:0:0:1") || host.startsWith("fc")
                || host.startsWith("fd") || host.startsWith("fe80:")) {
            return true;
        }
        if (!host.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            return false;
        }
        String[] octets = host.split("\\.");
        int first = Integer.parseInt(octets[0]);
        int second = Integer.parseInt(octets[1]);
        return first == 0 || first == 10 || first == 127 || first == 169 && second == 254
                || first == 172 && second >= 16 && second <= 31
                || first == 192 && second == 168;
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
