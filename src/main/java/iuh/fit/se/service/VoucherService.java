package iuh.fit.se.service;

import iuh.fit.se.dto.request.ApplyVoucherRequest;
import iuh.fit.se.dto.request.VoucherCreateRequest;
import iuh.fit.se.dto.request.VoucherValidationRequest;
import iuh.fit.se.dto.response.SellerVoucherResponse;
import iuh.fit.se.dto.response.UserVoucherResponse;
import iuh.fit.se.dto.response.VoucherResponse;
import iuh.fit.se.dto.response.VoucherValidationResponse;

import java.math.BigDecimal;
import java.util.List;

public interface VoucherService {
    public void claimVoucher(String voucherCode, String userId);
    public VoucherValidationResponse validateVoucher(VoucherValidationRequest request);
    public String applyVoucher(ApplyVoucherRequest request);
    public void completePaymentIntent(String paymentIntentId);
    public List<UserVoucherResponse> getMyVouchers(String userId);
    public VoucherResponse createVoucher(VoucherCreateRequest request);
    public void deleteVoucher(String id);
    public List<SellerVoucherResponse> getSellerVouchersForUser(String sellerId, String userId);
    public List<UserVoucherResponse> getUsableVouchersForCheckout(String userId, String sellerId, BigDecimal orderAmount);
    public void rollbackVoucher(String voucherCode,String orderId);
    public List<VoucherResponse> getAllVouchersBySeller(String sellerId);
}
