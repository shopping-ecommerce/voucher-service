package iuh.fit.se.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "voucher_usages",
        indexes = {
                @Index(name = "idx_voucher_user", columnList = "voucher_id, user_id"),
                @Index(name = "idx_order", columnList = "order_id", unique = true),
                @Index(name = "idx_user", columnList = "user_id")
        })
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoucherUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "voucher_id", nullable = false)
    String voucherId;

    @Column(name = "voucher_code", nullable = false)
    String voucherCode;

    @Column(name = "user_id", nullable = false)
    String userId;

    @Column(name = "order_id", nullable = false)
    String orderId;

    @Column(name = "discount_amount", precision = 19, scale = 2)
    BigDecimal discountAmount;

    @Column(name = "order_amount", precision = 19, scale = 2)
    BigDecimal orderAmount;

    @Column(name = "used_time")
    LocalDateTime usedTime;

    @PrePersist
    void prePersist() {
        this.usedTime = LocalDateTime.now();
    }
}