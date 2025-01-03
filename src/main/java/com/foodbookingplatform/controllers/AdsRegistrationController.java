package com.foodbookingplatform.controllers;

import com.foodbookingplatform.models.constants.AppConstants;
import com.foodbookingplatform.models.enums.OfferStatus;
import com.foodbookingplatform.models.payload.dto.adsregistration.AdsRegistrationAddEditResponse;
import com.foodbookingplatform.models.payload.dto.adsregistration.AdsRegistrationLocationResponse;
import com.foodbookingplatform.models.payload.dto.adsregistration.AdsRegistrationAddRequest;
import com.foodbookingplatform.models.payload.dto.adsregistration.AdsRegistrationResponse;
import com.foodbookingplatform.services.AdsRegistrationService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;

@RestController
@RequestMapping("/api/v1/ads-registrations")
@RequiredArgsConstructor
public class AdsRegistrationController {

    private final AdsRegistrationService adsRegistrationService;

    @ApiResponse(responseCode = "200", description = "Http Status 200 OK")
    @SecurityRequirement(name = "Bear Authentication")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @GetMapping
    public ResponseEntity<Page<AdsRegistrationLocationResponse>> getAllAdsRegistrations(
            @RequestParam(name = "pageNo", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.DEFAULT_SORT_BY, required = false) String sortBy,
            @RequestParam(name = "sortDir", defaultValue = AppConstants.DEFAULT_SORT_DIRECTION, required = false) String sortDir
    ) {
        return ResponseEntity.ok(adsRegistrationService.getAllAdsRegistrations(pageNo, pageSize, sortBy, sortDir));
    }

    @ApiResponse(responseCode = "200", description = "Http Status 200 OK")
    @SecurityRequirement(name = "Bear Authentication")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'LOCATION_ADMIN')")
    @GetMapping("/location/{locationId}")
    public ResponseEntity<AdsRegistrationLocationResponse> getAllAdsRegistrationOfLocation(@PathVariable Long locationId) throws AccessDeniedException {
        return ResponseEntity.ok(adsRegistrationService.getAdsRegistrationsOfLocation(locationId));
    }

    @ApiResponse(responseCode = "200", description = "Http Status 200 OK")
    @SecurityRequirement(name = "Bear Authentication")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'LOCATION_ADMIN')")
    @GetMapping("{id}")
    public ResponseEntity<AdsRegistrationResponse> getAdsRegistration(@PathVariable Long id) throws AccessDeniedException {
        return ResponseEntity.ok(adsRegistrationService.getAdsRegistration(id));
    }

    @ApiResponse(responseCode = "200", description = "Http Status 200 OK")
    @SecurityRequirement(name = "Bear Authentication")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'LOCATION_ADMIN')")
    @PostMapping
    public ResponseEntity<AdsRegistrationAddEditResponse> addAdsRegistration(@RequestBody @Valid AdsRegistrationAddRequest request) throws AccessDeniedException {
        return ResponseEntity.ok(adsRegistrationService.addAdsRegistration(request));
    }

    @ApiResponse(responseCode = "200", description = "Http Status 200 OK")
    @SecurityRequirement(name = "Bear Authentication")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'LOCATION_ADMIN')")
    @PutMapping
    public ResponseEntity<AdsRegistrationAddEditResponse> updateAdsRegistrationOfLocation(@RequestParam Long id, @RequestParam OfferStatus offerStatus) throws AccessDeniedException {
        return ResponseEntity.ok(adsRegistrationService.updateAdsRegistrationOfLocation(id, offerStatus));
    }

    @ApiResponse(responseCode = "200", description = "Http Status 200 OK")
    @SecurityRequirement(name = "Bear Authentication")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'LOCATION_ADMIN')")
    @DeleteMapping
    public ResponseEntity<String> deleteAdsRegistrationOfLocation(@RequestParam Long id) throws AccessDeniedException {
        adsRegistrationService.deleteAdsRegistrationOfLocation(id);
        return ResponseEntity.ok("Ads Registration deleted successfully");

    }
}
