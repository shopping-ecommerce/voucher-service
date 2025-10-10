package iuh.fit.se.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerVoucherResponse {
    private String id;
    private String code;
    private String name;
    private String description;
    private String type;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private Integer totalQuantity;
    private Integer claimedQuantity;
    private Integer remainingQuantity;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String applicableTo;
    private List<String> applicableIds;
    private String status;

    // Thông tin của user hiện tại
    private boolean userClaimed;           // User đã claim chưa
    private String userVoucherStatus;      // CLAIMED, USED, EXPIRED (nếu đã claim)
    private LocalDateTime userClaimedTime; // Thời gian user claim
    private LocalDateTime userUsedTime;    // Thời gian user dùng
    private boolean canClaim;              // User có thể claim không
    private boolean canUse;                // User có thể dùng không
    private String message;                // Thông báo cho user
}
