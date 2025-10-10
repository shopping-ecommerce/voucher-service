package iuh.fit.se.repository;

import iuh.fit.se.entity.Voucher;
import iuh.fit.se.entity.UserVoucher;
import iuh.fit.se.entity.VoucherUsage;
import iuh.fit.se.entity.enums.UserVoucherStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, String> {

    Optional<Voucher> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Voucher v WHERE v.code = :code")
    Optional<Voucher> findByCodeWithLock(@Param("code") String code);

    @Query("""
        SELECT v FROM Voucher v 
        WHERE v.status = 'ACTIVE' 
        AND v.startDate <= :now 
        AND v.endDate >= :now
        AND (v.totalQuantity IS NULL OR v.claimedQuantity < v.totalQuantity)
        ORDER BY v.createdTime DESC
    """)
    List<Voucher> findAvailableVouchers(@Param("now") LocalDateTime now);

    @Query("""
        SELECT v FROM Voucher v 
        WHERE v.status = 'ACTIVE' 
        AND v.startDate <= :now 
        AND v.endDate >= :now
        AND (v.totalQuantity IS NULL OR v.claimedQuantity < v.totalQuantity)
        AND (v.createdBy = :sellerId OR v.applicableTo = 'ALL')
        ORDER BY v.createdTime DESC
    """)
    List<Voucher> findAvailableVouchersBySeller(@Param("now") LocalDateTime now,
                                                @Param("sellerId") String sellerId);

    List<Voucher> findByCreatedBy(String createdBy);

    boolean existsByCode(String code);


}