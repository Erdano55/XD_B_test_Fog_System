package com.example.fog.service;

import com.example.fog.dto.Forecast7DTO;
import com.example.fog.dto.LocationDTO;
import com.example.fog.dto.WeatherNowDTO;
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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String qweatherKey;
    private final String qweatherNowUrl;
    private final String forecastUrl;
    private final String forecastAppid;
    private final String forecastAppsecret;

    public WeatherService(RestTemplate restTemplate,
                          ObjectMapper objectMapper,
                          @Value("${weather.qweather.key}") String qweatherKey,
                          @Value("${weather.qweather.now-url}") String qweatherNowUrl,
                          @Value("${weather.forecast.url}") String forecastUrl,
                          @Value("${weather.forecast.appid}") String forecastAppid,
                          @Value("${weather.forecast.appsecret}") String forecastAppsecret) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.qweatherKey = qweatherKey;
        this.qweatherNowUrl = qweatherNowUrl;
        this.forecastUrl = forecastUrl;
        this.forecastAppid = forecastAppid;
        this.forecastAppsecret = forecastAppsecret;
    }

    public WeatherNowDTO getNowWeather(LocationDTO location) {
        try {
            String locationParam = formatQweatherLocation(location);
            String url = UriComponentsBuilder.fromHttpUrl(qweatherNowUrl)
                    .queryParam("location", locationParam)
                    .queryParam("key", qweatherKey)
                    .build(false)
                    .toUriString();

            log.info("QWeather now weather request url: {}", url);
            String responseBody = getResponseBody(url);
            log.info("QWeather now weather raw response body: {}", responseBody);
            Map<String, Object> response = parseJsonToMap(responseBody, "QWeather now weather");

            checkQweatherCode(response, "QWeather now weather");
            Map<?, ?> now = getRequiredMap(response, "now", "QWeather now data is missing");

            return WeatherNowDTO.builder()
                    .temp(toStringValue(now.get("temp")))
                    .text(toStringValue(now.get("text")))
                    .build();
        } catch (RestClientException | IOException ex) {
            throw new IllegalStateException("Call QWeather now weather API failed: " + getDeepMessage(ex), ex);
        }
    }

    public Forecast7DTO getForecast7() {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(forecastUrl)
                    .queryParam("unescape", 1)
                    .queryParam("version", "v9")
                    .queryParam("appid", forecastAppid)
                    .queryParam("appsecret", forecastAppsecret)
                    .build()
                    .toUri();

            Map<?, ?> response = restTemplate.getForObject(uri, Map.class);
            Object dataObject = response == null ? null : response.get("data");
            if (!(dataObject instanceof List<?> forecastList)) {
                throw new IllegalStateException("Forecast data array is missing, response=" + response);
            }

            List<String> dates = new ArrayList<>();
            List<Integer> maxTemps = new ArrayList<>();
            List<Integer> minTemps = new ArrayList<>();
            List<Integer> humidity = new ArrayList<>();

            // The experiment requires a 7-day trend, so only the first seven rows are used.
            forecastList.stream().limit(7).forEach(item -> {
                if (item instanceof Map<?, ?> day) {
                    dates.add(toStringValue(day.get("date")));
                    maxTemps.add(parseWeatherNumber(day.get("tem1")));
                    minTemps.add(parseWeatherNumber(day.get("tem2")));
                    humidity.add(parseWeatherNumber(day.get("humidity")));
                }
            });

            return Forecast7DTO.builder()
                    .dates(dates)
                    .maxTemps(maxTemps)
                    .minTemps(minTemps)
                    .humidity(humidity)
                    .build();
        } catch (RestClientException ex) {
            throw new IllegalStateException("Call 7-day forecast API failed: " + ex.getMessage(), ex);
        }
    }

    private void checkQweatherCode(Map<?, ?> response, String apiName) {
        String code = toStringValue(response == null ? null : response.get("code"));
        if (!code.isEmpty() && !"200".equals(code)) {
            log.error("{} returned non-200 response: {}", apiName, response);
            throw new IllegalStateException(apiName + " returned code " + code + ", response=" + response);
        }
    }

    private Map<?, ?> getRequiredMap(Map<?, ?> response, String key, String message) {
        Object value = response == null ? null : response.get(key);
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalStateException(message + ", response=" + response);
        }
        return map;
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
            log.info("QWeather now weather response is gzip compressed, decompressing manually");
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

    private Integer parseWeatherNumber(Object value) {
        if (value == null) {
            return 0;
        }
        String text = String.valueOf(value).replaceAll("[^0-9-]", "");
        if (text.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(text);
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
