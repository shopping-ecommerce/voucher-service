 package iuh.fit.se.repository;

import iuh.fit.se.entity.PaymentIntent;
import iuh.fit.se.entity.enums.PaymentIntentStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, String> {

    /**
     * Phương thức này được sử dụng bởi Cron Job để tìm tất cả các "ý định thanh toán"
     * đang ở trạng thái PENDING và đã quá thời gian hết hạn.
     * Spring Data JPA sẽ tự động tạo câu lệnh SQL từ tên của phương thức.
     *
     * @param status Trạng thái cần tìm (sẽ là PENDING).
     * @param now    Thời gian hiện tại.
     * @return Danh sách các PaymentIntent đã hết hạn.
     */
    List<PaymentIntent> findAllByStatusAndExpiresAtBefore(PaymentIntentStatusEnum status, LocalDateTime now);

}