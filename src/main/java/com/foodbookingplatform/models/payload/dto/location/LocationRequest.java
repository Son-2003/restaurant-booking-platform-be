package com.foodbookingplatform.models.payload.dto.location;

import com.foodbookingplatform.models.constants.AppConstants;
import com.foodbookingplatform.models.enums.EntityStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LocationRequest {
    private Long id;

    @NotBlank(message = "Location's name cannot be blank")
    @Size(min = 2, message = "Location's name must have at least 2 characters")
    private String name;

    @NotBlank(message = "Location's address cannot be blank")
    @Size(min = 2, message = "Location's address must have at least 2 characters")
    private String address;

    @NotBlank(message = "Location's phone cannot be blank")
    @Pattern(regexp = AppConstants.PHONE_REGEX, message = "Invalid phone number!")
    @Size(min = 2, message = "Location's phone must have at least 2 characters")
    private String phone;

    @NotNull(message = "Location's latitude cannot be null")
    @Min(value = -90, message = "Latitude must be between -90 and 90")
    @Max(value = 90, message = "Latitude must be between -90 and 90")
    private double latitude;

    @NotNull(message = "Location's longitude cannot be null")
    @Min(value = -180, message = "Longitude must be between -180 and 180")
    @Max(value = 180, message = "Longitude must be between -180 and 180")
    private double longitude;

    @NotBlank(message = "Location's description cannot be blank")
    @Size(min = 2, message = "Location's description must have at least 2 characters")
    private String description;

    @NotBlank(message = "Location's image cannot be blank")
    @Size(min = 2, message = "Location's image must have at least 2 characters")
    private String image;

    private EntityStatus status;

    @NotNull(message = "UserId cannot be null")
    private Long userId;

    @NotNull(message = "BrandId cannot be null")
    private Long brandId;

    @NotNull(message = "BrandId cannot be null")
    private List<Long> categoryId;

    @NotNull(message = "BrandId cannot be null")
    private List<Long> tagId;
}
