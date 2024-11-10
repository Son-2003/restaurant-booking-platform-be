package com.foodbookingplatform.controllers;

import com.foodbookingplatform.models.constants.AppConstants;
import com.foodbookingplatform.models.enums.AdsType;
import com.foodbookingplatform.models.enums.EntityStatus;
import com.foodbookingplatform.models.payload.dto.location.LocationRequest;
import com.foodbookingplatform.models.payload.dto.location.LocationResponse;
import com.foodbookingplatform.models.payload.dto.location.LocationResponseLazy;
import com.foodbookingplatform.services.LocationService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {
    private final LocationService locationService;

    @ApiResponse(responseCode = "200", description = "Http Status 200 OK")
    @SecurityRequirement(name = "Bear Authentication")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @GetMapping
    public ResponseEntity<Page<LocationResponseLazy>> getAllLocations(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_BY, required = false) String sortBy,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_DIRECTION, required = false) String sortDir
    ) {
        return ResponseEntity.ok(locationService.getAllLocations(pageNo, pageSize, sortBy, sortDir));
    }

    @ApiResponse(responseCode = "200", description = "Http Status 200 OK")
    @SecurityRequirement(name = "Bear Authentication")
    @GetMapping("/ads-type")
    public ResponseEntity<Page<LocationResponseLazy>> getLocationsWithAdsType(@Valid
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam AdsType adsType
            ) {
        return ResponseEntity.ok(locationService.getLocationsWithBannerAds(pageNo, pageSize, adsType));
    }

    @ApiResponse(responseCode = "200", description = "Http Status 200 OK")
    @SecurityRequirement(name = "Bear Authentication")
    @GetMapping("/recommend")
    public ResponseEntity<Page<LocationResponseLazy>> getLocationsRecommend(@RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
                                                                            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize
    ) {
        return ResponseEntity.ok(locationService.getLocationsRecommend(pageNo, pageSize));
    }

    @ApiResponse(responseCode = "200", description = "Http Status 200 OK")
    @SecurityRequirement(name = "Bear Authentication")
    @GetMapping("/tag")
    public ResponseEntity<Page<LocationResponseLazy>> getLocationsWithTag(@RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
                                                                          @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
                                                                          @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_BY, required = false) String sortBy,
                                                                          @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_DIRECTION, required = false) String sortDir,
                                                                          @RequestParam String tagName
    ) {
        return ResponseEntity.ok(locationService.getLocationsWithTag(pageNo, pageSize, sortBy, sortDir, tagName));
    }

    @ApiResponse(responseCode = "200", description = "Http Status 200 OK")
    @SecurityRequirement(name = "Bear Authentication")
    @GetMapping("/search")
    public ResponseEntity<Page<LocationResponseLazy>> searchLocations(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_BY, required = false) String sortBy,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_DIRECTION, required = false) String sortDir,
            @RequestParam(required = false) List<EntityStatus> status,
            @RequestParam(required = false) String searchText,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) double latitude,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) double longitude,
            @RequestParam boolean searchNearBy
    ) {

        Map<String, Object> searchParams = new HashMap<>();

        if (status != null && !status.isEmpty()) searchParams.put("status", status);
        if (searchText != null) searchParams.put("searchText", searchText);

        return ResponseEntity.ok(locationService.searchAllLocations(pageNo, pageSize, sortBy, sortDir, searchParams, latitude, longitude, searchNearBy));
    }

    @ApiResponse(responseCode = "200", description = "Http Status 200 OK")
    @SecurityRequirement(name = "Bear Authentication")
    @GetMapping("{id}")
    public ResponseEntity<LocationResponse> getLocation(@PathVariable Long id) {
        return ResponseEntity.ok(locationService.getLocation(id));
    }

    @ApiResponse(responseCode = "201", description = "Http Status 201 Created")
    @SecurityRequirement(name = "Bear Authentication")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @PostMapping
    public ResponseEntity<LocationResponse> addLocation(@Valid @RequestBody LocationRequest request) {
        LocationResponse response = locationService.addLocation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @ApiResponse(responseCode = "200", description = "Http Status 200 OK")
    @SecurityRequirement(name = "Bear Authentication")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'LOCATION_ADMIN')")
    @PutMapping
    public ResponseEntity<LocationResponse> updateLocation(@Valid @RequestBody LocationRequest request) {
        return ResponseEntity.ok(locationService.updateLocation(request));
    }

    @ApiResponse(responseCode = "200", description = "Http Status 200 OK")
    @SecurityRequirement(name = "Bear Authentication")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @DeleteMapping("{id}")
    public ResponseEntity<String> deleteLocation(@PathVariable Long id) {
        locationService.deleteLocation(id);
        return ResponseEntity.ok("Location deleted successfully");
    }

    @ApiResponse(responseCode = "200", description = "Http Status 200 OK")
    @SecurityRequirement(name = "Bear Authentication")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @GetMapping("count-active-locations")
    public ResponseEntity<Integer> getNumberOfActiveLocationsInSystem() {
        return ResponseEntity.ok(locationService.getNumberOfActiveLocationsInSystem());
    }
}
