package com.foodbookingplatform.services.impl;

import com.foodbookingplatform.models.entities.*;
import com.foodbookingplatform.models.enums.EntityStatus;
import com.foodbookingplatform.models.enums.LocationBookingStatus;
import com.foodbookingplatform.models.exception.ResourceNotFoundException;
import com.foodbookingplatform.models.exception.RestaurantBookingException;
import com.foodbookingplatform.models.payload.dto.foodbooking.FoodBookingRequest;
import com.foodbookingplatform.models.payload.dto.foodbooking.FoodBookingResponse;
import com.foodbookingplatform.models.payload.dto.locationbooking.LocationBookingRequest;
import com.foodbookingplatform.models.payload.dto.locationbooking.LocationBookingResponse;
import com.foodbookingplatform.repositories.*;
import com.foodbookingplatform.services.FoodBookingService;
import com.foodbookingplatform.services.LocationBookingService;
import com.foodbookingplatform.services.VoucherService;
import com.foodbookingplatform.utils.GenericSpecification;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class LocationBookingServiceImpl implements LocationBookingService {

    private final LocationBookingRepository locationBookingRepository;
    private final LocationRepository locationRepository;
    private final FoodRepository foodRepository;
    private final UserRepository userRepository;
    private final VoucherRepository voucherRepository;
    private final FoodBookingService foodBookingService;
    private final VoucherService voucherService;
    private final ModelMapper mapper;

    @Override
    public Page<LocationBookingResponse> getAllBooking(int pageNo, int pageSize, String sortBy, String sortDir, Map<String, Object> keyword) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<LocationBooking> bookings;

        if(!keyword.isEmpty()) {
            Specification<LocationBooking> specification = specification(keyword);
            bookings = locationBookingRepository.findAll(specification, pageable);
        }else {
            bookings = locationBookingRepository.findAll(pageable);
        }

        return bookings.map(this::mapLocationBookingResponse);

    }

    @Override
    public Page<LocationBookingResponse> getAllBookingByLocation(Long locationId, int pageNo, int pageSize, String sortBy, String sortDir, Map<String, Object> keyword) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<LocationBooking> bookings;

        if(!keyword.isEmpty()) {
            Specification<LocationBooking> specification = specification(keyword);
            bookings = locationBookingRepository.findAllByLocationId(locationId, specification, pageable);
        }else {
            bookings = locationBookingRepository.findAllByLocationId(locationId, pageable);
        }

        return bookings.map(this::mapLocationBookingResponse);
    }

    @Override
    public Page<LocationBookingResponse> getAllBookingByUser(int pageNo, int pageSize, String sortBy, String sortDir, Map<String, Object> keyword) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<LocationBooking> bookings;


        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        if(!keyword.isEmpty()) {
            Specification<LocationBooking> specification = specification(keyword);
            bookings = locationBookingRepository.findAllByUser(user, specification, pageable);
        }else {
            bookings = locationBookingRepository.findAllByUser(user, pageable);
        }

        return bookings.map(this::mapLocationBookingResponse);
    }

    @Override
    public LocationBookingResponse createLocationBooking(LocationBookingRequest request) {
        LocationBooking newBooking = new LocationBooking();
        List<Food> orderedFoods = new ArrayList<>();
        List<Integer> foodQuantity = new ArrayList<>();
        float bookingAmount;
        float totalPrice = 0.0f;

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        Location bookedLocation = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new RestaurantBookingException(HttpStatus.BAD_REQUEST, "No restaurant available!"));
        if(!bookedLocation.getStatus().equals(EntityStatus.ACTIVE))
            throw new RestaurantBookingException(HttpStatus.BAD_REQUEST, "Restaurant is not available for booking!");

        if(request.getBookingDate().isAfter(LocalDate.now())){
            if(request.getBookingTime().isAfter(LocalTime.now())){
                newBooking.setName(request.getName());
                newBooking.setAddress(request.getAddress());
                newBooking.setPhone(request.getPhone());
                newBooking.setBookingDate(request.getBookingDate());
                newBooking.setBookingTime(request.getBookingTime());
                newBooking.setNumberOfAdult(request.getNumberOfAdult());
                newBooking.setNumberOfChildren(request.getNumberOfChildren());
                newBooking.setNumberOfGuest(request.getNumberOfAdult() + request.getNumberOfChildren());
                newBooking.setLocation(bookedLocation);
                newBooking.setUser(user);
                newBooking.setStatus(LocationBookingStatus.PENDING);
                newBooking = locationBookingRepository.save(newBooking);

                if(request.getPromotionId() != null){

                }

                if(!request.getFoodBookings().isEmpty()){
                    for (FoodBookingRequest f : request.getFoodBookings()) {
                        Food orderedFood = foodRepository.findById(f.getFoodId())
                                .orElseThrow(() -> new ResourceNotFoundException("Food", "id", f.getFoodId()));
                        if(!orderedFood.getLocation().equals(bookedLocation))
                            throw new RestaurantBookingException(HttpStatus.BAD_REQUEST, orderedFood.getName() + " is not available for this restaurant!");
                        if(!orderedFood.getStatus().equals(EntityStatus.ACTIVE))
                            throw new RestaurantBookingException(HttpStatus.BAD_REQUEST, orderedFood.getName() + " is not available to pre-order!");
                        orderedFoods.add(orderedFood);
                        foodQuantity.add(f.getQuantity());
                        totalPrice += orderedFood.getPrice();
                    }

                    List<FoodBooking> bookedFoods = foodBookingService.createFoodBooking(orderedFoods, foodQuantity, newBooking);

                    if(request.getVoucherId() != null){
                        Voucher voucher = voucherRepository.findById(request.getVoucherId())
                                .orElseThrow(() -> new ResourceNotFoundException("Voucher", "id", request.getVoucherId()));
                        bookingAmount = voucherService.applyVoucher(request.getVoucherId(), totalPrice);
                        newBooking.setAmount(bookingAmount);
                        newBooking.setVoucher(voucher);
                        voucher.getLocationBookings().add(newBooking);
                        voucherRepository.save(voucher);
                    }else throw new RestaurantBookingException(HttpStatus.BAD_REQUEST, "You have to pre-order foods in order to apply voucher!");
                    newBooking.setFoodBookings(new HashSet<>(bookedFoods));
                    newBooking = locationBookingRepository.save(newBooking);
                }
                return mapLocationBookingResponse(newBooking);

            }else throw new RestaurantBookingException(HttpStatus.BAD_REQUEST, "You cannot book the day and time before now!");
        }else throw new RestaurantBookingException(HttpStatus.BAD_REQUEST, "You cannot book the day before today!");
    }

    @Override
    public LocationBookingResponse cancelLocationBooking(Long bookingId) {
        LocationBooking locationBooking = locationBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        if(locationBooking.getStatus().equals(LocationBookingStatus.PENDING) ||
                locationBooking.getStatus().equals(LocationBookingStatus.CONFIRMED)){
            locationBooking.setStatus(LocationBookingStatus.CANCELLED);
            return mapLocationBookingResponse(locationBookingRepository.save(locationBooking));
        }else throw new RestaurantBookingException(HttpStatus.BAD_REQUEST, "This booking cannot be cancelled!");
    }

    @Override
    public LocationBookingResponse approveLocationBooking(Long bookingId) {
        LocationBooking locationBooking = locationBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        if(locationBooking.getStatus().equals(LocationBookingStatus.PENDING)){
            locationBooking.setStatus(LocationBookingStatus.CONFIRMED);
            return mapLocationBookingResponse(locationBookingRepository.save(locationBooking));
        }else throw new RestaurantBookingException(HttpStatus.BAD_REQUEST, "Only pending bookings are able to be approved");
    }

    @Override
    public LocationBookingResponse getLocationBookingDetail(Long bookingId) {
        LocationBooking locationBooking = locationBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        return mapLocationBookingResponse(locationBooking);
    }




    private Specification<LocationBooking> specification(Map<String, Object> searchParams){
        List<Specification<LocationBooking>> specs = new ArrayList<>();

        searchParams.forEach((key, value) -> {
            switch (key) {
                case "status":
                    specs.add(GenericSpecification.fieldIn(key, (Collection<?>) value));
                    break;
                case "startDate":
                    if (searchParams.containsKey("endDate")) {
                        specs.add(GenericSpecification.fieldBetween("bookingDate", (LocalDate) searchParams.get("startDate"), (LocalDate) searchParams.get("endDate")));
                    } else {
                        specs.add(GenericSpecification.fieldGreaterThan("bookingDate", (LocalDate) value));
                    }
                    break;
                case "endDate":
                    if (!searchParams.containsKey("startDate")) {
                        specs.add(GenericSpecification.fieldLessThan("bookingDate", (LocalDate) value));
                    }
                    break;
                case "startTime":
                    if (searchParams.containsKey("endTime")) {
                        specs.add(GenericSpecification.fieldBetween("bookingTime", (LocalTime) searchParams.get("startTime"), (LocalTime) searchParams.get("endTime")));
                    } else {
                        specs.add(GenericSpecification.fieldGreaterThan("bookingTime", (LocalTime) value));
                    }
                    break;
                case "endTime":
                    if (!searchParams.containsKey("startTime")) {
                        specs.add(GenericSpecification.fieldLessThan("bookingTime", (LocalTime) value));
                    }
                    break;
                case "startNumberOfGuest":
                    if (searchParams.containsKey("endNumberOfGuest")) {
                        specs.add(GenericSpecification.fieldBetween("numberOfGuest", (Integer) searchParams.get("startNumberOfGuest"), (Integer) searchParams.get("endNumberOfGuest")));
                    } else {
                        specs.add(GenericSpecification.fieldGreaterThan("numberOfGuest", (LocalTime) value));
                    }
                    break;
                case "endNumberOfGuest":
                    if (!searchParams.containsKey("startNumberOfGuest")) {
                        specs.add(GenericSpecification.fieldLessThan("numberOfGuest", (LocalTime) value));
                    }
                    break;
                case "name":
                case "address":
                case "phone":
                    specs.add(GenericSpecification.fieldContains(key, (String) value));
                    break;
            }
        });

        return specs.stream().reduce(Specification.where(null), Specification::and);
    }

    private LocationBookingResponse mapLocationBookingResponse(LocationBooking booking){
        LocationBookingResponse response = new LocationBookingResponse();
        List<FoodBookingResponse> foodBookingResponses = new ArrayList<>();
        Set<FoodBooking> foodBookings = booking.getFoodBookings();

        mapper.map(booking, response);

        if(!foodBookings.isEmpty()){
            foodBookings.forEach(foodBooking -> {
                foodBookingResponses.add(mapFoodBookingResponse(foodBooking));
            });
        }
        response.setFoodBookings(foodBookingResponses);
        return response;
    }

    private FoodBookingResponse mapFoodBookingResponse(FoodBooking booking){
        FoodBookingResponse response = new FoodBookingResponse();
        response.setFoodId(booking.getFood().getId());
        response.setFoodName(booking.getFood().getName());
        response.setQuantity(booking.getQuantity());
        response.setAmount(booking.getAmount());
        return response;
    }
}
