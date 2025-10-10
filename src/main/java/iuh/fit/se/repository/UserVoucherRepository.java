package iuh.fit.se.repository;

import iuh.fit.se.entity.UserVoucher;
import iuh.fit.se.entity.enums.UserVoucherStatusEnum;
import iuh.fit.se.entity.enums.VoucherStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserVoucherRepository extends JpaRepository<UserVoucher, String> {

    boolean existsByUserIdAndVoucherId(String userId, String voucherId);

    long countByVoucherId(String voucherId);

    @Query("""
        SELECT uv FROM UserVoucher uv 
        JOIN Voucher v ON uv.voucherId = v.id 
        WHERE uv.userId = :userId 
        AND v.code = :code
    """)
    Optional<UserVoucher> findByUserIdAndVoucherCode(@Param("userId") String userId,
                                                     @Param("code") String code);

    Optional<UserVoucher> findByUserIdAndVoucherId(String userId, String voucherId);

    @Query("""
        SELECT uv FROM UserVoucher uv 
        WHERE uv.userId = :userId 
        ORDER BY uv.claimedTime DESC
    """)
    List<UserVoucher> findByUserIdOrderByClaimedTimeDesc(@Param("userId") String userId);

    @Query("""
        SELECT uv FROM UserVoucher uv 
        WHERE uv.userId = :userId 
        AND uv.status = :status
        ORDER BY uv.claimedTime DESC
    """)
    List<UserVoucher> findByUserIdAndStatus(@Param("userId") String userId,
                                            @Param("status") UserVoucherStatusEnum status);

    @Query("""
        SELECT COUNT(uv) FROM UserVoucher uv 
        WHERE uv.userId = :userId 
        AND uv.voucherId = :voucherId
    """)
    long countByUserIdAndVoucherId(@Param("userId") String userId,
                                   @Param("voucherId") String voucherId);

    // Trong file: iuh/fit/se/repository/UserVoucherRepository.java
    @Query("SELECT uv FROM UserVoucher uv JOIN FETCH Voucher v ON uv.voucherId = v.id " +
            "WHERE uv.userId = :userId " +
            "AND uv.status = iuh.fit.se.entity.enums.UserVoucherStatusEnum.CLAIMED " +
            "AND v.createdBy = :sellerId")
    List<UserVoucher> findAllClaimedByUserIdAndSellerId(@Param("userId") String userId, @Param("sellerId") String sellerId);
}