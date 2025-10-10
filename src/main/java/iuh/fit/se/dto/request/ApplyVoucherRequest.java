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
public class ApplyVoucherRequest {
    private String voucherId;
    private String userId;
    private String orderId;
    private BigDecimal discountAmount;
    private BigDecimal orderAmount;
}
