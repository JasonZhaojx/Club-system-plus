package com.backend.sever.service;

import com.backend.pojo.vo.PlaceAutocompleteVO;
import com.backend.pojo.vo.PlaceDetailVO;

import java.math.BigDecimal;
import java.util.List;

public interface GooglePlacesService {
    List<PlaceAutocompleteVO> autocomplete(String input, BigDecimal latitude, BigDecimal longitude, String sessionToken);

    PlaceDetailVO getPlaceDetail(String placeId, String sessionToken);
}
