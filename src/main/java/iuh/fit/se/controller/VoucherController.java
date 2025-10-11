package iuh.fit.se.controller;

import iuh.fit.se.dto.request.*;
import iuh.fit.se.dto.response.*;
import iuh.fit.se.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    /**
     * User claim voucher - CHỈ ĐƯỢC CLAIM 1 LẦN
     * POST /api/vouchers/claim?voucherCode=SUMMER2024&userId=xxx
     */
    @PostMapping("/claim")
    public ApiResponse<Void> claimVoucher(
            @RequestParam String voucherCode,
            @RequestParam String userId) {
        voucherService.claimVoucher(voucherCode, userId);

        return ApiResponse.<Void>builder()
                .code(200)
                .message("Nhận voucher thành công")
                .build();
    }

    /**
     * Validate voucher trước khi tạo order
     * POST /api/vouchers/validate
     */
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<VoucherValidationResponse>> validateVoucher(
            @RequestBody VoucherValidationRequest request) {
        VoucherValidationResponse data = voucherService.validateVoucher(request);

        ApiResponse<VoucherValidationResponse> response = ApiResponse.<VoucherValidationResponse>builder()
                .code(200)
                .message("Validate thành công")
                .result(data)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Apply voucher khi tạo order thành công
     * POST /api/vouchers/apply
     */
    @PostMapping("/apply")
    public ApiResponse<String> applyVoucher(
            @RequestBody ApplyVoucherRequest request) {


        return ApiResponse.<String>builder()
                .code(200)
                .message("Áp dụng voucher thành công")
                .result(voucherService.applyVoucher(request))
                .build();
    }

    @GetMapping("/complete/{paymentIntentId}")
    public ApiResponse<String> completeVoucher(@PathVariable String paymentIntentId) {
        voucherService.completePaymentIntent(paymentIntentId);
        return ApiResponse.<String>builder()
                .code(200)
                .message("Hoàn tất sử dụng voucher thành công")
                .build();
    }
    /**
     * Lấy danh sách voucher của user (đã claim)
     * GET /api/vouchers/my-vouchers?userId=xxx
     */
    @GetMapping("/my-vouchers")
    public ResponseEntity<ApiResponse<?>> getMyVouchers(@RequestParam String userId) {
        var data = voucherService.getMyVouchers(userId);

        ApiResponse<?> response = ApiResponse.builder()
                .code(200)
                .message("Lấy danh sách voucher của bạn thành công")
                .result(data)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Admin/Seller tạo voucher
     * POST /api/vouchers
     */
    @PostMapping("/create")
    public ApiResponse<VoucherResponse> createVoucher(
            @RequestBody VoucherCreateRequest request) {
        VoucherResponse data = voucherService.createVoucher(request);

    return ApiResponse.<VoucherResponse>builder()
                .code(200)
                .message("Tạo voucher thành công")
                .result(data)
                .build();

    }

    /**
     * Admin/Seller cập nhật voucher
     * PUT /api/vouchers/{id}
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<VoucherResponse>> updateVoucher(
            @PathVariable String id,
            @RequestBody VoucherUpdateRequest request) {
//        VoucherResponse data = voucherService.updateVoucher(id, request);

        ApiResponse<VoucherResponse> response = ApiResponse.<VoucherResponse>builder()
                .code(200)
                .message("Cập nhật voucher thành công")
//                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Admin/Seller xóa voucher
     * DELETE /api/vouchers/{id}
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVoucher(@PathVariable String id) {
        voucherService.deleteVoucher(id);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(200)
                .message("Xóa voucher thành công")
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/seller/{sellerId}")
    public ApiResponse<List<SellerVoucherResponse>> getSellerVouchersForUser(
            @PathVariable String sellerId,
            @RequestParam String userId) {
        List<SellerVoucherResponse> data = voucherService.getSellerVouchersForUser(sellerId, userId);
        return ApiResponse.<List<SellerVoucherResponse>>builder()
                .code(200)
                .message("Lấy danh sách voucher thành công")
                .result(data)
                .build();
    }

    @GetMapping("/usable-vouchers")
    public ResponseEntity<List<UserVoucherResponse>> getVouchersForCheckout(
            @RequestParam String userId,
            @RequestParam String sellerId,
            @RequestParam BigDecimal orderAmount) {

        List<UserVoucherResponse> usableVouchers = voucherService.getUsableVouchersForCheckout(userId, sellerId, orderAmount);
        return ResponseEntity.ok(usableVouchers);
    }

    @GetMapping("/seller/all/{sellerId}")
    public ApiResponse<List<VoucherResponse>> getAllVouchersBySeller(
            @PathVariable String sellerId) {
        List<VoucherResponse> data = voucherService.getAllVouchersBySeller(sellerId);
        return ApiResponse.<List<VoucherResponse>>builder()
                .code(200)
                .message("Lấy danh sách voucher thành công")
                .result(data)
                .build();
    }
}