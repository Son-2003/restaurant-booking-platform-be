package com.foodbookingplatform.services.impl;

import com.foodbookingplatform.models.entities.*;
import com.foodbookingplatform.models.enums.DayInWeek;
import com.foodbookingplatform.models.enums.EntityStatus;
import com.foodbookingplatform.models.enums.LocationBookingStatus;
import com.foodbookingplatform.models.exception.ResourceNotFoundException;
import com.foodbookingplatform.models.exception.RestaurantBookingException;
import com.foodbookingplatform.models.payload.dto.foodbooking.FoodBookingRequest;
import com.foodbookingplatform.models.payload.dto.foodbooking.FoodBookingResponse;
import com.foodbookingplatform.models.payload.dto.locationbooking.LocationBookingRequest;
import com.foodbookingplatform.models.payload.dto.locationbooking.LocationBookingResponse;
import com.foodbookingplatform.models.payload.dto.promotion.CheckPromotionResponse;
import com.foodbookingplatform.models.payload.dto.uservoucher.CheckVoucherResponse;
import com.foodbookingplatform.repositories.*;
import com.foodbookingplatform.services.*;
import com.foodbookingplatform.utils.GenericSpecification;
import com.foodbookingplatform.utils.SecurityUtils;
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

import java.nio.file.AccessDeniedException;
import java.time.DayOfWeek;
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
    private final PromotionRepository promotionRepository;
    private final WorkingHourRepository workingHourRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final FoodBookingService foodBookingService;
    private final VoucherService voucherService;
    private final PromotionService promotionService;
    private final EmailService emailService;
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
    public Page<LocationBookingResponse> getAllBookingByLocation(Long locationId, int pageNo, int pageSize, String sortBy, String sortDir, Map<String, Object> keyword) throws AccessDeniedException {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<LocationBooking> bookings;

        if(!SecurityUtils.isAuthorizeLocation(locationId, userRepository))
            throw new RestaurantBookingException(HttpStatus.NOT_FOUND, "You dont have this location with id: " + locationId);

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
        float voucherDiscountAmount = 0.0f;
        float promotionDiscountAmount = 0.0f;
        float totalPrice = 0.0f;

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        Location bookedLocation = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new RestaurantBookingException(HttpStatus.BAD_REQUEST, "No restaurant available!"));
        if(!bookedLocation.getStatus().equals(EntityStatus.ACTIVE))
            throw new RestaurantBookingException(HttpStatus.BAD_REQUEST, "Restaurant is not available for booking!");

        String dayInWeek = String.valueOf(request.getBookingDate().getDayOfWeek());
        WorkingHour workingHour = workingHourRepository.findByLocation_IdAndDay(bookedLocation.getId(), DayInWeek.valueOf(dayInWeek));
        if(request.getBookingTime().isBefore(workingHour.getStartTime()) || request.getBookingTime().isAfter(workingHour.getEndTime()))
            throw new RestaurantBookingException(HttpStatus.BAD_REQUEST, "Please book another time!");

        if(request.getBookingDate().isBefore(LocalDate.now())) {
            throw new RestaurantBookingException(HttpStatus.BAD_REQUEST, "You cannot book in the past!");
        } else if(request.getBookingDate().isEqual(LocalDate.now()) && request.getBookingTime().isBefore(LocalTime.now())) {
            throw new RestaurantBookingException(HttpStatus.BAD_REQUEST, "You cannot book in the past!");
        }

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

        if(!request.getFoodBookings().isEmpty()) {
            for (int i = 0; i < request.getFoodBookings().size(); i++) {
                FoodBookingRequest foodBookingRequest = request.getFoodBookings().get(i);
                Food orderedFood = foodRepository.findById(foodBookingRequest.getFoodId())
                        .orElseThrow(() -> new ResourceNotFoundException("Food", "id", foodBookingRequest.getFoodId()));
                if (!orderedFood.getLocation().equals(bookedLocation))
                    throw new RestaurantBookingException(HttpStatus.BAD_REQUEST, orderedFood.getName() + " is not available for this restaurant!");
                if (!orderedFood.getStatus().equals(EntityStatus.ACTIVE))
                    throw new RestaurantBookingException(HttpStatus.BAD_REQUEST, orderedFood.getName() + " is not available to pre-order!");
                orderedFoods.add(orderedFood);
                foodQuantity.add(foodBookingRequest.getQuantity());
                totalPrice += orderedFood.getPrice() * foodBookingRequest.getQuantity();
            }
        }

        List<FoodBooking> bookedFoods = foodBookingService.createFoodBooking(orderedFoods, foodQuantity, newBooking);

        if(request.getPromotionId() != null && request.getPromotionId() != 0 ){
            Promotion promotion = promotionRepository.findById(request.getPromotionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Promotion", "id", request.getPromotionId()));
            CheckPromotionResponse checkPromotionResponse = promotionService.applyPromotion(request.getPromotionId(), totalPrice,
                    newBooking.getNumberOfGuest(), newBooking.getBookingDate(), newBooking.getBookingTime());
            promotionDiscountAmount = checkPromotionResponse.getDiscountedValue();
            newBooking.setPromotion(promotion);
            promotion.getLocationBookings().add(newBooking);
            promotionRepository.save(promotion);
        }

        if(request.getVoucherId() != null && request.getVoucherId() != 0){
            UserVoucher userVoucher = userVoucherRepository.findByVoucher_IdAndUserUserName(request.getVoucherId(), username)
                    .orElseThrow(() -> new ResourceNotFoundException("Voucher", "id", request.getVoucherId()));
            Voucher voucher = userVoucher.getVoucher();
            CheckVoucherResponse checkVoucherResponse = voucherService.applyVoucher(request.getVoucherId(), totalPrice);
            voucherDiscountAmount = checkVoucherResponse.getDiscountedValue();
            newBooking.setVoucher(voucher);
            voucher.getLocationBookings().add(newBooking);
            userVoucher.setQuantityAvailable(userVoucher.getQuantityAvailable() - 1);
            userVoucherRepository.save(userVoucher);
            voucherRepository.save(voucher);
        }

        newBooking.setFoodBookings(new HashSet<>(bookedFoods));
        newBooking.setAmount(totalPrice - promotionDiscountAmount - voucherDiscountAmount);
        newBooking = locationBookingRepository.save(newBooking);
        sendMailCreateBooking(newBooking);
        return mapLocationBookingResponse(newBooking);
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
            locationBooking = locationBookingRepository.save(locationBooking);
            sendMailApproveBooking(locationBooking);
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

    private void sendMailApproveBooking(LocationBooking locationBooking) {
        String subject = "[SkedEat Thông báo Đặt Chỗ]: Đặt Chỗ Của Bạn Đã Được Xác Nhận!";
        String content = String.format(
                "Kính gửi %s,\n\nChúng tôi vui mừng thông báo rằng đơn đặt chỗ của bạn với nhà hàng **%d** đã được xác nhận thành công!\n\n" +
                        "**Chi tiết Đặt Chỗ:**\n" +
                        "- **Địa điểm:** %s\n" +
                        "- **Ngày:** %s\n" +
                        "- **Giờ:** %s\n" +
                        "- **Trạng thái:** Đã xác nhận\n\n" +
                        "Cảm ơn bạn đã chọn chúng tôi cho nhu cầu đặt chỗ của bạn. Nếu bạn có bất kỳ câu hỏi nào hoặc cần hỗ trợ thêm, xin vui lòng liên hệ với chúng tôi.\n\n" +
                        "Trân trọng,\n" +
                        "[Tên Công Ty của Bạn]\n" +
                        "[Thông Tin Liên Hệ của Công Ty]",
                locationBooking.getUser().getFullName(),  // Giả sử bạn có phương thức getName() cho người dùng
                locationBooking.getLocation().getName(),
                locationBooking.getLocation().getName(),   // Giả sử bạn có phương thức để lấy tên địa điểm
                locationBooking.getBookingDate(),      // Giả sử bạn có phương thức để lấy ngày đặt chỗ
                locationBooking.getBookingTime()        // Giả sử bạn có phương thức để lấy giờ đặt chỗ
        );

        emailService.sendEmail(locationBooking.getUser().getEmail(), subject, content);

    }

    private void sendMailCreateBooking(LocationBooking locationBooking){
        String subject = "[SkedEat Thông Báo Đặt Chỗ]: Đơn Đặt Chỗ Của Bạn Đang Chờ Xác Nhận!";
        String content = String.format(
                "Kính gửi %s,\n\nChúng tôi xin thông báo rằng đơn đặt chỗ của bạn với nhà hàng **%s** hiện đang chờ xác nhận.\n\n" +
                        "**Chi tiết Đặt Chỗ:**\n" +
                        "- **Địa điểm:** %s\n" +
                        "- **Ngày:** %s\n" +
                        "- **Giờ:** %s\n" +
                        "- **Trạng thái:** Đang chờ xác nhận\n\n" +
                        "Chúng tôi sẽ thông báo cho bạn ngay khi đơn đặt chỗ của bạn được xác nhận. Nếu bạn có bất kỳ câu hỏi nào hoặc cần hỗ trợ thêm, xin vui lòng liên hệ với chúng tôi.\n\n" +
                        "Trân trọng,\n" +
                        "[Tên Công Ty của Bạn]\n" +
                        "[Thông Tin Liên Hệ của Công Ty]",
                locationBooking.getUser().getFullName(),  // Giả sử bạn có phương thức getName() cho người dùng
                locationBooking.getLocation().getName() ,
                locationBooking.getLocation().getName() ,     // Giả sử bạn có phương thức để lấy tên địa điểm
                locationBooking.getBookingDate(),      // Giả sử bạn có phương thức để lấy ngày đặt chỗ
                locationBooking.getBookingTime()        // Giả sử bạn có phương thức để lấy giờ đặt chỗ
        );

        emailService.sendEmail(locationBooking.getUser().getEmail(), subject, content);

    }

    private LocationBookingResponse mapLocationBookingResponse(LocationBooking booking){
        LocationBookingResponse response = new LocationBookingResponse();
        List<FoodBookingResponse> foodBookingResponses = new ArrayList<>();
        Set<FoodBooking> foodBookings = booking.getFoodBookings();

        mapper.map(booking, response);
        if(booking.getPromotion() != null) response.setFreeItem(booking.getPromotion().getFreeItem());
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
