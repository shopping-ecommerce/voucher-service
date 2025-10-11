package iuh.fit.se.repository;

import iuh.fit.se.entity.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, String> {

    Optional<VoucherUsage> findByOrderId(String orderId);

    List<VoucherUsage> findByUserId(String userId);

    List<VoucherUsage> findByVoucherId(String voucherId);

    long countByUserIdAndVoucherId(String userId, String voucherId);

    @Query("""
        SELECT vu FROM VoucherUsage vu 
        WHERE vu.userId = :userId 
        ORDER BY vu.usedTime DESC
    """)
    List<VoucherUsage> findByUserIdOrderByUsedTimeDesc(@Param("userId") String userId);

    boolean existsByOrderId(String orderId);
    Optional<VoucherUsage> findByVoucherCodeAndOrderId(String voucherCode,String orderId);

}