package com.example.fog.service;

import com.example.fog.dto.LocationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Set;

@Service
public class LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationService.class);

    private static final Set<String> XIAN_AREA_NAMES = Set.of(
            "YuHuaZhai", "Yuhuazhai", "鱼化寨", "雁塔", "雁塔区", "未央", "未央区",
            "莲湖", "莲湖区", "碑林", "碑林区", "新城", "新城区", "灞桥", "灞桥区",
            "长安", "长安区", "临潼", "临潼区", "阎良", "阎良区", "高陵", "高陵区",
            "鄠邑", "鄠邑区", "蓝田", "蓝田县", "周至", "周至县"
    );

    private final RestTemplate restTemplate;
    private final String ipApiUrl;

    public LocationService(RestTemplate restTemplate, @Value("${location.ip-api-url}") String ipApiUrl) {
        this.restTemplate = restTemplate;
        this.ipApiUrl = ipApiUrl;
    }

    public LocationDTO getLocationByIp() {
        try {
            Map<?, ?> response = restTemplate.getForObject(ipApiUrl, Map.class);
            log.info("IP location raw response: {}", response);

            if (response == null || response.get("lat") == null || response.get("lon") == null) {
                throw new IllegalStateException("IP location API did not return valid latitude and longitude, response=" + response);
            }
            if ("fail".equalsIgnoreCase(toStringValue(response.get("status")))) {
                throw new IllegalStateException("IP location API failed, response=" + response);
            }

            Double lon = toDouble(response.get("lon"));
            Double lat = toDouble(response.get("lat"));
            String rawCity = toStringValue(response.get("city"));
            String regionName = toStringValue(response.get("regionName"));
            String country = toStringValue(response.get("country"));
            String city = normalizeCity(rawCity, regionName, country, lon, lat);

            return LocationDTO.builder()
                    .lon(lon)
                    .lat(lat)
                    .city(city)
                    .rawCity(rawCity)
                    .regionName(regionName)
                    .country(country)
                    .ip(toStringValue(firstNonNull(response.get("query"), response.get("ip"))))
                    .build();
        } catch (RestClientException ex) {
            throw new IllegalStateException("Call IP location API failed: " + ex.getMessage(), ex);
        }
    }

    private String normalizeCity(String rawCity, String regionName, String country, Double lon, Double lat) {
        if (isXiAn(rawCity, regionName, country, lon, lat)) {
            return "西安";
        }
        return rawCity.isBlank() ? regionName : rawCity;
    }

    private boolean isXiAn(String rawCity, String regionName, String country, Double lon, Double lat) {
        String cityText = rawCity == null ? "" : rawCity.trim();
        String regionText = regionName == null ? "" : regionName.trim();

        if (containsXiAn(cityText) || containsXiAn(regionText)) {
            return true;
        }
        if (XIAN_AREA_NAMES.contains(cityText)) {
            return true;
        }

        boolean inChina = country == null || country.isBlank()
                || "China".equalsIgnoreCase(country)
                || "中国".equals(country)
                || "中国大陆".equals(country);
        boolean inXiAnBoundingBox = lon != null && lat != null
                && lon >= 107.6 && lon <= 109.8
                && lat >= 33.6 && lat <= 34.9;

        return inChina && inXiAnBoundingBox;
    }

    private boolean containsXiAn(String text) {
        return text.contains("西安")
                || text.contains("Xi'an")
                || text.contains("Xian")
                || text.contains("XiAn");
    }

    private Double toDouble(Object value) {
        return value == null ? null : Double.parseDouble(String.valueOf(value));
    }

    private String toStringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }
}
