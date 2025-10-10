package iuh.fit.se.config;

import iuh.fit.se.entity.PaymentIntent;
import iuh.fit.se.entity.UserVoucher;
import iuh.fit.se.entity.Voucher;
import iuh.fit.se.entity.enums.PaymentIntentStatusEnum;
import iuh.fit.se.entity.enums.UserVoucherStatusEnum;
import iuh.fit.se.repository.PaymentIntentRepository;
import iuh.fit.se.repository.UserVoucherRepository;
import iuh.fit.se.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherCleanup {
    private final PaymentIntentRepository paymentIntentRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final VoucherRepository voucherRepository;
    // Scheduled to run every 15 minutes: "0 */15 * * * *"
    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void rollbackExpiredIntents() {
        // 1. Find PENDING intents that have expired
        List<PaymentIntent> expiredIntents = paymentIntentRepository
                .findAllByStatusAndExpiresAtBefore(PaymentIntentStatusEnum.PENDING, LocalDateTime.now());

        for (PaymentIntent intent : expiredIntents) {
            try {
                // 2. Update intent status to EXPIRED
                intent.setStatus(PaymentIntentStatusEnum.EXPIRED);
                paymentIntentRepository.save(intent);

                // 3. Rollback: Update UserVoucher status to CLAIMED
                if (intent.getUserVoucherId() != null) {
                    UserVoucher userVoucher = userVoucherRepository.findById(intent.getUserVoucherId()).orElse(null);
                    if (userVoucher != null && userVoucher.getStatus() == UserVoucherStatusEnum.USED) {
                        userVoucher.setStatus(UserVoucherStatusEnum.CLAIMED);
                        userVoucherRepository.save(userVoucher);
                        log.info("Rolled back UserVoucher: {}", userVoucher.getId());
                    }
                }
                if(intent.getVoucherId() != null){
                    log.info("Rolled back Voucher: {}", intent.getVoucherId());
                    Voucher voucher = voucherRepository.findById(intent.getVoucherId()).orElse(null);
                    if(voucher != null){
                        voucher.setUsedQuantity(voucher.getUsedQuantity() - 1);
                        voucherRepository.save(voucher);
                    }
                }
            } catch (Exception e) {
                log.error("Error processing PaymentIntent with ID {}: {}", intent.getId(), e.getMessage(), e);
            }
        }
    }
}