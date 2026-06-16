package com.backend.sever.controller;

import com.backend.pojo.vo.PlaceAutocompleteVO;
import com.backend.pojo.vo.PlaceDetailVO;
import com.backend.sever.common.Result;
import com.backend.sever.service.GooglePlacesService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/geo")
public class GeoController {
    private final GooglePlacesService googlePlacesService;

    public GeoController(GooglePlacesService googlePlacesService) {
        this.googlePlacesService = googlePlacesService;
    }

    @GetMapping("/places/autocomplete")
    public Result<List<PlaceAutocompleteVO>> autocomplete(
            @RequestParam String input,
            @RequestParam(required = false) BigDecimal lat,
            @RequestParam(required = false) BigDecimal lng,
            @RequestParam(required = false) String sessionToken
    ) {
        return Result.success(googlePlacesService.autocomplete(input, lat, lng, sessionToken));
    }

    @GetMapping("/places/{placeId}")
    public Result<PlaceDetailVO> getPlaceDetail(
            @PathVariable String placeId,
            @RequestParam(required = false) String sessionToken
    ) {
        return Result.success(googlePlacesService.getPlaceDetail(placeId, sessionToken));
    }
}
