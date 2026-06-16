package com.backend.sever.service.impl;

import com.backend.pojo.vo.PlaceAutocompleteVO;
import com.backend.pojo.vo.PlaceDetailVO;
import com.backend.sever.config.GoogleMapsProperties;
import com.backend.sever.exception.BusinessException;
import com.backend.sever.exception.ErrorCode;
import com.backend.sever.service.GooglePlacesService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GooglePlacesServiceImpl implements GooglePlacesService {
    private static final URI AUTOCOMPLETE_URI = URI.create("https://places.googleapis.com/v1/places:autocomplete");
    private static final String AUTOCOMPLETE_FIELD_MASK = "suggestions.placePrediction.placeId,suggestions.placePrediction.text,suggestions.placePrediction.structuredFormat";
    private static final String DETAILS_FIELD_MASK = "id,displayName,formattedAddress,location";

    private final GoogleMapsProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GooglePlacesServiceImpl(GoogleMapsProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();
    }

    @Override
    public List<PlaceAutocompleteVO> autocomplete(String input, BigDecimal latitude, BigDecimal longitude, String sessionToken) {
        ensureApiKey();
        if (!StringUtils.hasText(input) || input.trim().length() < 2) {
            return List.of();
        }
        try {
            Map<String, Object> body = buildAutocompleteBody(input.trim(), latitude, longitude, sessionToken);
            HttpRequest request = HttpRequest.newBuilder(AUTOCOMPLETE_URI)
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/json")
                    .header("X-Goog-Api-Key", properties.apiKey())
                    .header("X-Goog-FieldMask", AUTOCOMPLETE_FIELD_MASK)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            JsonNode root = send(request);
            List<PlaceAutocompleteVO> results = new ArrayList<>();
            for (JsonNode suggestion : root.path("suggestions")) {
                JsonNode prediction = suggestion.path("placePrediction");
                if (prediction.isMissingNode() || !prediction.hasNonNull("placeId")) {
                    continue;
                }
                PlaceAutocompleteVO item = new PlaceAutocompleteVO();
                item.setPlaceId(prediction.path("placeId").asText());
                item.setFullText(prediction.path("text").path("text").asText());
                item.setMainText(prediction.path("structuredFormat").path("mainText").path("text").asText(item.getFullText()));
                item.setSecondaryText(prediction.path("structuredFormat").path("secondaryText").path("text").asText(""));
                results.add(item);
            }
            return results;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Google Places 查询失败");
        }
    }

    @Override
    public PlaceDetailVO getPlaceDetail(String placeId, String sessionToken) {
        ensureApiKey();
        if (!StringUtils.hasText(placeId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "placeId 不能为空");
        }
        try {
            String encodedPlaceId = URLEncoder.encode(placeId.trim(), StandardCharsets.UTF_8);
            String sessionPart = StringUtils.hasText(sessionToken)
                    ? "?sessionToken=" + URLEncoder.encode(sessionToken.trim(), StandardCharsets.UTF_8)
                    : "";
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://places.googleapis.com/v1/places/" + encodedPlaceId + sessionPart))
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/json")
                    .header("X-Goog-Api-Key", properties.apiKey())
                    .header("X-Goog-FieldMask", DETAILS_FIELD_MASK)
                    .GET()
                    .build();
            JsonNode root = send(request);
            PlaceDetailVO detail = new PlaceDetailVO();
            detail.setPlaceId(root.path("id").asText(placeId));
            detail.setDisplayName(root.path("displayName").path("text").asText(""));
            detail.setFormattedAddress(root.path("formattedAddress").asText(""));
            if (!root.path("location").hasNonNull("latitude") || !root.path("location").hasNonNull("longitude")) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "该地点没有可用坐标");
            }
            detail.setLatitude(root.path("location").path("latitude").decimalValue());
            detail.setLongitude(root.path("location").path("longitude").decimalValue());
            return detail;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Google Place Detail 查询失败");
        }
    }

    private Map<String, Object> buildAutocompleteBody(String input, BigDecimal latitude, BigDecimal longitude, String sessionToken) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("input", input);
        body.put("languageCode", "en");
        body.put("regionCode", "AU");
        body.put("includedRegionCodes", List.of("au"));
        body.put("includeQueryPredictions", false);
        if (StringUtils.hasText(sessionToken)) {
            body.put("sessionToken", sessionToken.trim());
        }
        if (latitude != null && longitude != null) {
            body.put("locationBias", Map.of(
                    "circle", Map.of(
                            "center", Map.of(
                                    "latitude", latitude,
                                    "longitude", longitude
                            ),
                            "radius", 5000.0
                    )
            ));
        }
        return body;
    }

    private JsonNode send(HttpRequest request) throws IOException {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Google Places API 返回错误: " + response.statusCode());
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Google Places 请求被中断");
        }
    }

    private void ensureApiKey() {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "未配置 GOOGLE_MAPS_API_KEY");
        }
    }
}
