package iuh.fit.se.entity;

import iuh.fit.se.entity.enums.PaymentIntentStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payment_intents",
        indexes = {
                // Index quan trọng cho Cron Job dọn dẹp
                @Index(name = "idx_status_expires_at", columnList = "status, expires_at")
        })
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentIntent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id; // Sẽ được dùng làm vnp_TxnRef gửi qua VNPay

    @Column(name = "voucher_id", nullable = false)
    String voucherId;

    @Column(name = "user_voucher_id") // Nullable vì có thể thanh toán không dùng voucher
    String userVoucherId; // ID của UserVoucher đã bị "khóa"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    PaymentIntentStatusEnum status;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    LocalDateTime expiresAt; // Thời điểm hết hạn để cron job xử lý
    @PrePersist
    void prePersist() {
        if (this.status == null) {
            this.status = PaymentIntentStatusEnum.PENDING;
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}