package com.foodbookingplatform.services.impl;

import com.foodbookingplatform.models.constants.AppConstants;
import com.foodbookingplatform.models.entities.*;
import com.foodbookingplatform.models.enums.EntityStatus;
import com.foodbookingplatform.models.exception.ResourceNotFoundException;
import com.foodbookingplatform.models.exception.RestaurantBookingException;
import com.foodbookingplatform.models.payload.dto.category.CategoryResponse;
import com.foodbookingplatform.models.payload.dto.location.LocationRequest;
import com.foodbookingplatform.models.payload.dto.location.LocationResponse;
import com.foodbookingplatform.models.payload.dto.tag.TagResponse;
import com.foodbookingplatform.repositories.*;
import com.foodbookingplatform.services.LocationService;
import com.foodbookingplatform.utils.GenericSpecification;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final LocationCategoryRepository locationCategoryRepository;
    private final LocationTagRepository locationTagRepository;
    private final ModelMapper mapper;

    @Override
    public LocationResponse addLocation(LocationRequest locationRequest) {
        Location savedLocation = validate(locationRequest, locationRequest.getId());
        return mapToResponse(savedLocation);
    }

    @Override
    public LocationResponse getLocation(Long id) {
        Location location = locationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Location", "id", id));
        return mapToResponse(location);
    }

    @Override
    public Page<LocationResponse> getAllLocations(int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Location> locationPage = locationRepository.findAll(pageable);
        return locationPage.map(this::mapToResponse);
    }

    @Override
    public Page<LocationResponse> searchAllLocations(int pageNo, int pageSize, String sortBy, String sortDir, Map<String, Object> searchParams) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Specification<Location> specification = specification(searchParams);

        Page<Location> locationPage = locationRepository.findAll(specification, pageable);

        return locationPage.map(this::mapToResponse);
    }

    @Override
    public LocationResponse updateLocation(LocationRequest locationRequest) {
        Location updatedLocation = validate(locationRequest, locationRequest.getId());
        return mapToResponse(updatedLocation);
    }

    @Override
    public void deleteLocation(long id) {
        Location location = locationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Location", "id", id));
        location.setStatus(EntityStatus.DISABLED);
        locationRepository.save(location);
    }

    private Specification<Location> specification(Map<String, Object> searchParams){
        List<Specification<Location>> specs = new ArrayList<>();

        searchParams.forEach((key, value) -> {
            switch (key) {
                case "status":
                    specs.add(GenericSpecification.fieldIn(key, (Collection<?>) value));
                    break;
                case "suggest":
                case "sale":
                    specs.add(GenericSpecification.fieldIsBoolean(key, (boolean) value));
                    break;
                case "openingHours":
                    if (searchParams.containsKey("closingHours")) {
                        specs.add(GenericSpecification.fieldBetween("closingHours", (LocalDateTime) searchParams.get("openingHours"), (LocalDateTime) searchParams.get("closingHours")));
                    } else {
                        specs.add(GenericSpecification.fieldGreaterThan("openingHours", (LocalDateTime) value));
                    }
                    break;
                case "closingHours":
                    if (!searchParams.containsKey("openingHours")) {
                        specs.add(GenericSpecification.fieldLessThan("closingHours", (LocalDateTime) value));
                    }
                    break;
                case "name":
                case "address":
                case "phone":
                    specs.add(GenericSpecification.fieldContains(key, (String) value));
                    break;
                case "fullName":
                    specs.add(GenericSpecification.joinFieldContains("user", key, (String) value));
                    break;
                case "brandName":
                    specs.add(GenericSpecification.joinFieldContains("brand", "name", (String) value));
                    break;
                case "categoryName":
                    specs.add(GenericSpecification.joinFieldInThroughMultipleJoins("locationCategories", "category", "name", (Collection<?>) value));
                    break;
                case "tagName":
                    specs.add(GenericSpecification.joinFieldInThroughMultipleJoins("locationTags", "tag", "name", (Collection<?>) value));
                    break;
            }
        });

        return specs.stream().reduce(Specification.where(null), Specification::and);
    }

    public Location validate(LocationRequest locationRequest, Long locationId) {
        Location location;

        if (locationId != 0) {
            location = locationRepository.findById(locationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Location", "id", locationId));
            BeanUtils.copyProperties(locationRequest, location, "user", "brand");

        } else {
            location = mapper.map(locationRequest, Location.class);
        }

        User user = userRepository.findById(locationRequest.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", locationRequest.getUserId()));
        if (!user.getRole().getName().equalsIgnoreCase(AppConstants.ROLE_LOCATION)) {
            throw new RestaurantBookingException(HttpStatus.BAD_REQUEST, "User does not belong to role LOCATION_ADMIN");
        }

        Brand brand = brandRepository.findById(locationRequest.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Brand", "id", locationRequest.getBrandId()));

        location.setUser(user);
        location.setBrand(brand);
        Location savedLocation = locationRepository.save(location);



        if (!locationRequest.getCategoryId().isEmpty()) {
            // Lấy hết LocationCategory hiện có từ DB
            List<LocationCategory> currentLocationCategories = locationCategoryRepository.findByLocation(savedLocation);

            // Danh sách CategoryId hiện có trong LocationCategory
            List<Long> existingCategoryIds = currentLocationCategories.stream()
                    .map(lc -> lc.getCategory().getId())
                    .toList();

            // Xóa các Category không có trong danh sách từ Request
            List<LocationCategory> categoriesToRemove = currentLocationCategories.stream()
                    .filter(lc -> !locationRequest.getCategoryId().contains(lc.getCategory().getId()))
                    .toList();
            locationCategoryRepository.deleteAll(categoriesToRemove);

            // Thêm các Category mới
            List<LocationCategory> newLocationCategories = locationRequest.getCategoryId().stream()
                    .filter(categoryId -> !existingCategoryIds.contains(categoryId)) // Lọc các category mới cần thêm
                    .map(categoryId -> {
                        Category category = categoryRepository.findById(categoryId)
                                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
                        LocationCategory locationCategory = new LocationCategory();
                        locationCategory.setLocation(savedLocation);
                        locationCategory.setCategory(category);
                        return locationCategory;
                    })
                    .toList();
            locationCategoryRepository.saveAll(newLocationCategories);
        }

        if (!locationRequest.getTagId().isEmpty()) {
            // Lấy hết LocationTag hiện có từ DB
            List<LocationTag> currentLocationTags = locationTagRepository.findByLocation(savedLocation);

            // Danh sách TagId hiện có trong LocationTag
            List<Long> existingTagIds = currentLocationTags.stream()
                    .map(lt -> lt.getTag().getId())
                    .toList();

            // Xóa các Tag không có trong danh sách từ Request
            List<LocationTag> tagsToRemove = currentLocationTags.stream()
                    .filter(lt -> !locationRequest.getTagId().contains(lt.getTag().getId()))
                    .toList();
            locationTagRepository.deleteAll(tagsToRemove);

            // Thêm các Tag mới
            List<LocationTag> newLocationTags = locationRequest.getTagId().stream()
                    .filter(tagId -> !existingTagIds.contains(tagId)) // Lọc các tag mới cần thêm
                    .map(tagId -> {
                        Tag tag = tagRepository.findById(tagId)
                                .orElseThrow(() -> new ResourceNotFoundException("Tag", "id", tagId));
                        LocationTag locationTag = new LocationTag();
                        locationTag.setLocation(savedLocation);
                        locationTag.setTag(tag);
                        return locationTag;
                    })
                    .toList();
            locationTagRepository.saveAll(newLocationTags);
        }
        return savedLocation;
    }

    private LocationResponse mapToResponse(Location location){
        List<Category> categories = locationCategoryRepository.findCategoriesByLocationId(location.getId());
        List<Tag> tags = locationTagRepository.findTagsByLocationId(location.getId());
        LocationResponse locationResponse = mapper.map(location, LocationResponse.class);
        locationResponse.setCategory(categories.stream().map(category -> mapper.map(category, CategoryResponse.class)).toList());
        locationResponse.setTag(tags.stream().map(tag -> mapper.map(tag, TagResponse.class)).toList());
        return locationResponse;
    }
}