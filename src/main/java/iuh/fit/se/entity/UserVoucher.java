package iuh.fit.se.entity;

import iuh.fit.se.entity.enums.UserVoucherStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_vouchers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_voucher",
                        columnNames = {"user_id", "voucher_id"})
        },
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_voucher_id", columnList = "voucher_id")
        })
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserVoucher {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "user_id", nullable = false)
    String userId;

    @Column(name = "voucher_id", nullable = false)
    String voucherId;

    @Enumerated(EnumType.STRING)
    UserVoucherStatusEnum status; // CLAIMED, USED, EXPIRED

    @Column(name = "claimed_time")
    LocalDateTime claimedTime;

    @Column(name = "used_time")
    LocalDateTime usedTime;

    @Column(name = "order_id")
    String orderId; // Order đã sử dụng voucher này

    @PrePersist
    void prePersist() {
        this.status = UserVoucherStatusEnum.CLAIMED;
        this.claimedTime = LocalDateTime.now();
    }

    public boolean canBeUsed() {
        return status == UserVoucherStatusEnum.CLAIMED;
    }

    public void markAsUsed(String orderId) {
        this.status = UserVoucherStatusEnum.USED;
        this.usedTime = LocalDateTime.now();
        this.orderId = orderId;
    }
}
