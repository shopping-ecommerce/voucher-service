package iuh.fit.se.entity;

import iuh.fit.se.entity.enums.VoucherTypeEnum;
import iuh.fit.se.entity.enums.VoucherStatusEnum;
import iuh.fit.se.entity.enums.ApplicableToEnum;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
@Entity
@Table(name = "vouchers")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(unique = true, nullable = false)
    String code;

    @Column(nullable = false)
    String name;

    @Column(columnDefinition = "TEXT")
    String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    VoucherTypeEnum type;

    @Column(name = "discount_value", precision = 19, scale = 2)
    BigDecimal discountValue;

    @Column(name = "max_discount_amount", precision = 19, scale = 2)
    BigDecimal maxDiscountAmount;

    @Column(name = "min_order_amount", precision = 19, scale = 2)
    BigDecimal minOrderAmount;

    @Column(name = "total_quantity")
    Integer totalQuantity;

    @Column(name = "claimed_quantity")
    Integer claimedQuantity;

    @Column(name = "used_quantity")
    Integer usedQuantity;

    @Column(name = "start_date")
    LocalDateTime startDate;

    @Column(name = "end_date")
    LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "applicable_to")
    ApplicableToEnum applicableTo;

    @ElementCollection
    @CollectionTable(name = "voucher_applicable_ids", joinColumns = @JoinColumn(name = "voucher_id"))
    @Column(name = "applicable_id")
    List<String> applicableIds = new ArrayList<>();

    @Column(name = "created_by")
    String createdBy;

    @Enumerated(EnumType.STRING)
    VoucherStatusEnum status;

    @Column(name = "created_time")
    LocalDateTime createdTime;

    @Column(name = "modified_time")
    LocalDateTime modifiedTime;

    @PrePersist
    void prePersist() {
        this.claimedQuantity = 0;
        this.usedQuantity = 0;
        this.status = VoucherStatusEnum.ACTIVE;
        this.createdTime = LocalDateTime.now();
        this.modifiedTime = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.modifiedTime = LocalDateTime.now();
    }

    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return status != null && status == VoucherStatusEnum.ACTIVE
                && startDate != null && endDate != null
                && !now.isBefore(startDate)
                && now.isBefore(endDate)
                && (totalQuantity == null || (usedQuantity != null && usedQuantity < totalQuantity));
    }

    public boolean hasQuantityLeft() {
        return totalQuantity == null || claimedQuantity < totalQuantity;
    }
}