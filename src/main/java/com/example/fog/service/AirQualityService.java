package com.example.fog.service;

import com.example.fog.dto.AirQualityDTO;
import com.example.fog.dto.LocationDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

@Service
public class AirQualityService {

    private static final Logger log = LoggerFactory.getLogger(AirQualityService.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String qweatherKey;
    private final String qweatherAirUrl;

    public AirQualityService(RestTemplate restTemplate,
                             ObjectMapper objectMapper,
                             @Value("${weather.qweather.key}") String qweatherKey,
                             @Value("${weather.qweather.air-url}") String qweatherAirUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.qweatherKey = qweatherKey;
        this.qweatherAirUrl = qweatherAirUrl;
    }

    public AirQualityDTO getNowAirQuality(LocationDTO location) {
        try {
            String locationParam = formatQweatherLocation(location);
            String url = UriComponentsBuilder.fromHttpUrl(qweatherAirUrl)
                    .queryParam("location", locationParam)
                    .queryParam("key", qweatherKey)
                    .build(false)
                    .toUriString();

            log.info("QWeather air request url: {}", url);
            String responseBody = getResponseBody(url);
            log.info("QWeather air raw response body: {}", responseBody);
            Map<String, Object> response = parseJsonToMap(responseBody, "QWeather air");

            checkQweatherCode(response);
            Object nowObject = response == null ? null : response.get("now");
            if (!(nowObject instanceof Map<?, ?> now)) {
                throw new IllegalStateException("QWeather air now data is missing, response=" + response);
            }

            return AirQualityDTO.builder()
                    .category(toStringValue(now.get("category")))
                    .aqi(toStringValue(now.get("aqi")))
                    .pm25(toStringValue(now.get("pm2p5")))
                    .so2(toStringValue(now.get("so2")))
                    .no2(toStringValue(now.get("no2")))
                    .co(toStringValue(now.get("co")))
                    .o3(toStringValue(now.get("o3")))
                    .build();
        } catch (RestClientException | IOException ex) {
            throw new IllegalStateException("Call QWeather air API failed: " + getDeepMessage(ex), ex);
        }
    }

    private void checkQweatherCode(Map<?, ?> response) {
        String code = toStringValue(response == null ? null : response.get("code"));
        if (!code.isEmpty() && !"200".equals(code)) {
            log.error("QWeather air API returned non-200 response: {}", response);
            throw new IllegalStateException("QWeather air API returned code " + code + ", response=" + response);
        }
    }

    private Map<String, Object> parseJsonToMap(String responseBody, String apiName) throws JsonProcessingException {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException(apiName + " returned empty response body");
        }
        try {
            return objectMapper.readValue(responseBody, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            log.error("{} returned invalid JSON body: {}", apiName, responseBody, ex);
            throw ex;
        }
    }

    private String getResponseBody(String url) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, "application/json");
        headers.set(HttpHeaders.ACCEPT_ENCODING, "identity");

        ResponseEntity<byte[]> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                byte[].class
        );
        byte[] body = responseEntity.getBody();
        if (body == null || body.length == 0) {
            return "";
        }

        String contentEncoding = responseEntity.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING);
        if (isGzip(contentEncoding, body)) {
            log.info("QWeather air response is gzip compressed, decompressing manually");
            try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(body))) {
                return new String(gzipInputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        return new String(body, StandardCharsets.UTF_8);
    }

    private boolean isGzip(String contentEncoding, byte[] body) {
        boolean headerSaysGzip = contentEncoding != null && contentEncoding.toLowerCase(Locale.ROOT).contains("gzip");
        boolean bodyLooksGzip = body.length >= 2 && (body[0] == 0x1f) && ((body[1] & 0xff) == 0x8b);
        return headerSaysGzip || bodyLooksGzip;
    }

    private String formatQweatherLocation(LocationDTO location) {
        if (location == null || location.getLon() == null || location.getLat() == null) {
            throw new IllegalStateException("Location longitude or latitude is missing, location=" + location);
        }
        return String.format(Locale.US, "%.2f,%.2f", location.getLon(), location.getLat());
    }

    private String toStringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String getDeepMessage(Exception ex) {
        Throwable current = ex;
        Throwable deepest = ex;
        while (current != null) {
            deepest = current;
            current = current.getCause();
        }
        String message = deepest.getMessage();
        return message == null ? ex.getMessage() : message;
    }
}
