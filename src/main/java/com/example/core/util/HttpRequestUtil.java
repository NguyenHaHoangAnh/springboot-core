package com.example.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class HttpRequestUtil {
    private static final Logger log = LoggerFactory.getLogger(HttpRequestUtil.class);

    public static String sendGet(String url, String token, String xToken, int timeOut) throws Exception {
        return buildAndExecute(url, "", token, xToken, timeOut, REQUEST_TYPE.GET, "application/json");
    }

    public static String sendPost(String url, Object payload, String token, String xToken, int timeOut) throws Exception {
        return buildAndExecute(url, payload, token, xToken, timeOut, REQUEST_TYPE.POST, "application/json");
    }

    public static String sendPut(String url, Object payload, String token, String xToken, int timeOut) throws Exception {
        return buildAndExecute(url, payload, token, xToken, timeOut, REQUEST_TYPE.PUT, "application/json");
    }

    public static String sendDelete(String url, Object payload, String token, String xToken, int timeOut) throws Exception {
        return buildAndExecute(url, payload, token, xToken, timeOut, REQUEST_TYPE.DELETE, "application/json");
    }

    public static String buildAndExecute(String url, Object payload, String token, String xToken, int timeOut, REQUEST_TYPE requestType, String contentType) throws Exception {
        return buildAndExecute(url, payload, token, xToken, timeOut, requestType, contentType, false);
    }

    public static String buildAndExecute(String url, Object payload, String token, String xToken, int timeOut, REQUEST_TYPE requestType, String contentType, boolean exceptionOnError) throws Exception {
        // Thời gian gọi API
        long start = System.currentTimeMillis();
        // Tạo connection từ URL -> nhưng chưa phải HTTP/HTTPS, chỉ là giao thức chung
        URL objUrl = new URL(url);
        URLConnection connection = objUrl.openConnection();
        HttpsURLConnection httpsConnection = null;
        HttpURLConnection httpConnection = null;
        // Kiểm tra HTTP/HTTPS
        // HTTPS -> cần xử lý SSL
        // HTTP -> dùng HttpURLConnection bình thường
        boolean isHttps = connection instanceof HttpsURLConnection;
        if (isHttps) {
            // XỬ lý HTTPS và bỏ qua chứng chỉ SSL -> Quan trọng
            // Tạo SSLContext dạng TLS
            // Gắn vào 1 TrustManager tự tạo
            // TrustManager này ko kiểm tra gì cả -> Bỏ qua mọi chứng chỉ SSL
            // Sau đó, gắn socket SSL vào connection
            // => Cho phép gọi HTTPS ngay cả khi SSL certificate ko hợp lệ (self-signed, expired, ...)
            // => Không nên dùng trong production nếu ko cần thiết
            httpsConnection = (HttpsURLConnection) connection;
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, new SecureRandom());
            httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
        } else {
            httpConnection = (HttpURLConnection) connection;
        }
        // Ép về HttpURLConnection để cấu hình
        ((HttpURLConnection) (isHttps ? httpsConnection : httpConnection)).setRequestMethod(requestType.name());
        ((HttpURLConnection) (isHttps ? httpsConnection : httpConnection)).setRequestProperty("accept", "application/json");
        ((HttpURLConnection) (isHttps ? httpsConnection : httpConnection)).setRequestProperty("Content-Type", contentType);
        ((HttpURLConnection) (isHttps ? httpsConnection : httpConnection)).setRequestProperty("Authorization", token);
        ((HttpURLConnection) (isHttps ? httpsConnection : httpConnection)).setRequestProperty("X-Auth-Token", xToken);
        ((HttpURLConnection) (isHttps ? httpsConnection : httpConnection)).setConnectTimeout(timeOut * 1000);
        // Gửi payload khi ko phải GET
        // GET ko có body -> bỏ qua
        // POST/PUT/PATCH -> gửi JSON payload
        // strPayload = String/obj -> convert sang JSON = Util.getGson().toJson(payload)
        if (!REQUEST_TYPE.GET.equals(requestType)) {
            ((HttpURLConnection) (isHttps ? httpsConnection : httpConnection)).setDoOutput(true);
            OutputStreamWriter writer = null;

            try {
                writer = new OutputStreamWriter(((HttpURLConnection) (isHttps ? httpsConnection : httpConnection)).getOutputStream(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                Thread.sleep(5L);
                writer = new OutputStreamWriter(((HttpURLConnection) (isHttps ? httpsConnection : httpConnection)).getOutputStream(), StandardCharsets.UTF_8);
            }

            String strPayload = payload instanceof String ? String.valueOf(payload) : Utils.getGson().toJson(payload);
            writer.write(strPayload);
            writer.flush();
        }
        // Lấy response
        // - Nếu HTTP code 2xx -> Đọc input stream
        // - Nếu HTTP code 4xx/5xx -> Đọc error stream
        int responseCode = ((HttpURLConnection) (isHttps ? httpsConnection : httpConnection)).getResponseCode();
        BufferedReader reader;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(
                            ((HttpURLConnection) (isHttps ? httpsConnection : httpConnection)).getInputStream(),
                            StandardCharsets.UTF_8
            ));
        } catch (Exception e) {
            reader = new BufferedReader(
                    new InputStreamReader(
                            ((HttpURLConnection) (isHttps ? httpsConnection : httpConnection)).getInputStream(),
                            StandardCharsets.UTF_8
            ));
        }

        StringBuffer response = new StringBuffer();

        // Convert toàn bộ response -> string
        String inputLine;
        while ((inputLine = reader.readLine()) != null) {
            response.append(inputLine);
        }

        reader.close();
        String jsonResponse = response.toString();
        if (responseCode < 200 || responseCode > 299) {
            log.error("Http request: url={}, payload={}, response={}", new Object[]{url, null, jsonResponse});
            if (exceptionOnError) {
                throw new Exception("INTERNAL_SERVER_ERROR");
            }
        }

        log.info("Http request: url={}, method={}, status={}, time={}ms", new Object[]{url, requestType, responseCode, System.currentTimeMillis() - start});
        return jsonResponse;
    }

    public static enum REQUEST_TYPE {
        GET,
        POST,
        PUT,
        DELETE;
    }
}
