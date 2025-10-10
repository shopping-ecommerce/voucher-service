package iuh.fit.se.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVoucherResponse {
    private String id;
    private String voucherId;
    private String code;
    private String name;
    private String description;
    private String type;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private String status;
    private LocalDateTime claimedTime;
    private LocalDateTime usedTime;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean canUse;
}
