package iuh.fit.se.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SellerVoucherItemResponse {
    private VoucherResponse voucher; // tái dùng mapper sẵn có
    private boolean claimed;         // user đã nhận chưa?
    private boolean canClaim;        // có thể bấm "Nhận" không?
    private boolean canUse;          // nếu đã nhận: còn dùng được không?
}