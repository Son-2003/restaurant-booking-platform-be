package com.foodbookingplatform.services.impl;

import com.foodbookingplatform.models.entities.LocationBooking;
import com.foodbookingplatform.models.entities.Notification;
import com.foodbookingplatform.models.entities.User;
import com.foodbookingplatform.models.enums.EntityStatus;
import com.foodbookingplatform.models.enums.LocationBookingStatus;
import com.foodbookingplatform.models.enums.RoleType;
import com.foodbookingplatform.models.exception.ResourceNotFoundException;
import com.foodbookingplatform.models.payload.dto.notification.NotificationRequest;
import com.foodbookingplatform.models.payload.dto.notification.NotificationResponse;
import com.foodbookingplatform.repositories.LocationBookingRepository;
import com.foodbookingplatform.repositories.NotificationRepository;
import com.foodbookingplatform.repositories.UserRepository;
import com.foodbookingplatform.services.NotificationService;
import com.foodbookingplatform.utils.GenericSpecification;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final LocationBookingRepository locationBookingRepository;
    private final UserRepository userRepository;
    private final ModelMapper mapper;

    @Value("${webhook-url}")
    private String WEBHOOK_URL_PREFIX;

    @Override
    public List<NotificationResponse> addNotification(NotificationRequest notificationRequest) {
        List<Notification> notificationList = new ArrayList<>();
        List<Notification> notificationSaved = new ArrayList<>();

        if(notificationRequest.getUserId() == 0){
            List<User> users = userRepository.findByRoleNameContainsIgnoreCase(notificationRequest.getRecipientType());
            for(User user : users){
                Notification notification = mapper.map(notificationRequest, Notification.class);
                notification.setUser(user);
                notificationList.add(notification);
            }
            notificationSaved = notificationRepository.saveAll(notificationList);
        }
        else{
            User user = userRepository.findById(notificationRequest.getUserId()).orElseThrow(() -> new ResourceNotFoundException("User", "id", notificationRequest.getUserId()));
            Notification notification = mapper.map(notificationRequest, Notification.class);
            notification.setUser(user);
            notificationSaved.add(notificationRepository.save(notification));
        }

        return notificationSaved.stream().map(this::mapToResponse).toList();
    }

    @Override
    public Page<NotificationResponse> searchNotifications(int pageNo, int pageSize, String sortBy, String sortDir, Map<String, Object> searchParams) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Specification<Notification> specification = specification(searchParams);

        Page<Notification> notificationPage = notificationRepository.findAll(specification, pageable);

        return notificationPage.map(this::mapToResponse);
    }

    @Override
    public NotificationResponse getNotification(Long id) {
        Notification notification = notificationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
        return mapToResponse(notification);
    }

    @Override
    public Page<NotificationResponse> getAllNotifications(int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Notification> notificationPage = notificationRepository.findAll(pageable);
        return notificationPage.map(this::mapToResponse);
    }

    @Override
    public Page<NotificationResponse> getAllNotificationsOfUser(int pageNo, int pageSize, String sortBy, String sortDir, Long id) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Notification> notificationPage = notificationRepository.findAllByUser_Id(id, pageable);
        return notificationPage.map(this::mapToResponse);
    }

    @Override
    public NotificationResponse updateNotification(NotificationRequest notificationRequest) {
        Notification notification = notificationRepository.findById(notificationRequest.getId()).orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationRequest.getId()));
        mapper.map(notificationRequest, notification);
        return mapToResponse(notificationRepository.save(notification));
    }

    @Override
    public void deleteNotification(long id) {
        Notification notification = notificationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
        if (notification != null) {
            notification.setStatus(EntityStatus.DISABLED);
            notificationRepository.save(notification);
        }
    }

    public float calculateMonthlyPayment(Long userId, int month, int year) {
        List<LocationBooking> successfulBookings = locationBookingRepository.findAllSuccessfulBookings(
                userId, LocationBookingStatus.SUCCESSFUL, month, year);

        return (float) successfulBookings.stream()
                .mapToDouble(LocationBooking::getCommission)
                .sum();
    }

    public void sendWebhookNotification(int month, int year , int totalAmount) {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> payload = new HashMap<>();
        payload.put("month", month);
        payload.put("year", year);
        payload.put("totalAmount", totalAmount);

        restTemplate.postForEntity(WEBHOOK_URL_PREFIX + "/api/v1/notifications/commission-monthly-payment", payload, String.class);
    }

    @Scheduled(cron = "0 * * * * ?")  // Mỗi phút
    public void calculateAndSendMonthlyBilling() {
        String roleName = "LOCATION_ADMIN";
        List<Long> locationAdminIds = userRepository.findAllUserIdByRoleName(roleName);
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();
        // Lặp qua từng nhà hàng và gửi thông báo webhook
        for (Long id  : locationAdminIds) {
            float totalAmount = calculateMonthlyPayment(id, month, year);
            int roundedAmount = (int) Math.floor(totalAmount);
            sendWebhookNotification(month, year, roundedAmount);
        }
    }


    //map các dkien của user thành 1 specification
    private Specification<Notification> specification(Map<String, Object> searchParams){
        List<Specification<Notification>> specs = new ArrayList<>();

        // Lặp qua từng entry trong searchParams để tạo các Specification tương ứng
        searchParams.forEach((key, value) -> {
            switch (key) {
                case "status":
                    specs.add(GenericSpecification.fieldIn(key, (Collection<?>) value));
                    break;
                case "sendDateFrom":
                    // Nếu có cả orderDateFrom và orderDateTo, sử dụng fieldBetween để tạo Specification
                    if (searchParams.containsKey("sendDateTo")) {
                        specs.add(GenericSpecification.fieldBetween("sendDate", (LocalDateTime) searchParams.get("sendDateFrom"), (LocalDateTime) searchParams.get("sendDateTo")));
                    } else {
                        // Ngược lại, sử dụng fieldGreaterThan để tạo Specification
                        specs.add(GenericSpecification.fieldGreaterThan("sendDate", (LocalDateTime) value));
                    }
                    break;
                case "sendDateTo":
                    if (!searchParams.containsKey("sendDateFrom")) {
                        specs.add(GenericSpecification.fieldLessThan("sendDate", (LocalDateTime) value));
                    }
                    break;
                case "title":
                case "summary":
                case "content":
                case "recipientType":
                case "notificationType":
                    specs.add(GenericSpecification.fieldContains(key, (String) value));
                    break;
                case "fullName":
                    specs.add(GenericSpecification.joinFieldContains("user", key, (String) value));
                    break;
            }
        });

        // Tổng hợp tất cả các Specification thành một Specification duy nhất bằng cách sử dụng phương thức reduce của Stream
        //reduce de ket hop cac spec1(dk1), spec2(dk2),.. thanh 1 specification chung va cac spec ket hop voi nhau thono qua AND
        return specs.stream().reduce(Specification.where(null), Specification::and);
    }

    private NotificationResponse mapToResponse(Notification notification){
        NotificationResponse response = mapper.map(notification, NotificationResponse.class);
        response.setFullName(notification.getUser().getFullName());
        return response;
    }
}
