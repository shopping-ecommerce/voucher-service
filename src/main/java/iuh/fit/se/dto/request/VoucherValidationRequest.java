package iuh.fit.se.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherValidationRequest {
    private String voucherCode;
    private String userId;
    private BigDecimal orderAmount;      // Tổng tiền hóa đơn (subtotal)
    private BigDecimal shippingFee;      // Phí ship (optional)
    private String sellerId;             // Seller ID (optional)
}
