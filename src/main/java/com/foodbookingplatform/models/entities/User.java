package com.foodbookingplatform.models.entities;

import com.foodbookingplatform.models.enums.EntityStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Length;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "user_name")
    private String userName;

    @Column(nullable = false, name = "full_name")
    private String fullName;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String gender;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EntityStatus status;

    @Column(nullable = false)
    private long point = 0L;

    @Column(nullable = false)
    private boolean isFirstLogin;

    @Column(nullable = false, length = Length.LOB_DEFAULT)
    private String image;

    @Column(name = "created_date",nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdDate;

    @Column(name = "modified_date", insertable = false)
    private LocalDateTime modifiedDate;

    @PrePersist
    protected void onCreate() {
        ZonedDateTime nowInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        this.createdDate = nowInVietnam.toLocalDateTime();
        this.modifiedDate = nowInVietnam.toLocalDateTime();
    }

    @PreUpdate
    protected void onUpdate() {
        ZonedDateTime nowInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        this.modifiedDate = nowInVietnam.toLocalDateTime();
    }

    @OneToMany(mappedBy = "user")
    private Set<Token> tokens;

    @OneToMany(mappedBy = "user")
    private Set<Address> addresses;

    @OneToMany(mappedBy = "user")
    private Set<PointTransaction> pointTransactions;

    @OneToMany(mappedBy = "user")
    private Set<SystemBlog> systemBlogs;

    @OneToMany(mappedBy = "reporter")
    private Set<Report> reports;

    @OneToMany(mappedBy = "answerer")
    private Set<Report> reportAnswers;

    @OneToMany(mappedBy = "user")
    private Set<Notification> notifications;

    @OneToMany(mappedBy = "user")
    private Set<UserVoucher> userVouchers;

    @OneToMany(mappedBy = "user")
    private Set<LocationBooking> locationBookings;

    @OneToMany(mappedBy = "user")
    private Set<LocationFeedback> locationFeedbacks;

    @OneToMany(mappedBy = "user")
    private Set<Location> locations;

    @OneToMany(mappedBy = "user")
    private Set<PayOSTransaction> transactions;

    @ManyToOne
    @JoinColumn(name = "role_Id", nullable = false)
    private Role role;
}

