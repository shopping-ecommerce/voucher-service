package iuh.fit.se.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherValidationResponse {
    private boolean valid;
    private String message;
    private VoucherInfo voucherInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoucherInfo {
        private String voucherId;
        private String code;
        private String name;
        private BigDecimal discountAmount;
        private BigDecimal finalAmount;
    }
}
