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
    public void applyVoucher(ApplyVoucherRequest request);
    public List<VoucherResponse> getAvailableVouchers(String userId, String sellerId);
    public List<UserVoucherResponse> getMyVouchers(String userId);
    public List<UserVoucherResponse> getMyVouchersBySeller(String userId, String sellerId);
    public VoucherResponse createVoucher(VoucherCreateRequest request);
    public void deleteVoucher(String id);
    public List<SellerVoucherResponse> getSellerVouchersForUser(String sellerId, String userId);
    public List<UserVoucherResponse> getUsableVouchersForCheckout(String userId, String sellerId, BigDecimal orderAmount);

}
