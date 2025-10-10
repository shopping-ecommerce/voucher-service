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
    public ApiResponse<Void> applyVoucher(
            @RequestBody ApplyVoucherRequest request) {
        voucherService.applyVoucher(request);

        return ApiResponse.<Void>builder()
                .code(200)
                .message("Áp dụng voucher thành công")
                .build();
    }

    /**
     * Rollback voucher khi order bị hủy
     * POST /api/vouchers/rollback/{orderId}
     */
    @PostMapping("/rollback/{orderId}")
    public ResponseEntity<ApiResponse<Void>> rollbackVoucher(@PathVariable String orderId) {
//        voucherService.rollbackVoucher(orderId);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(200)
                .message("Hoàn trả voucher thành công")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách voucher available (chưa claim)
     * GET /api/vouchers/available?userId=xxx&sellerId=yyy (sellerId optional)
     */
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<?>> getAvailableVouchers(
            @RequestParam String userId,
            @RequestParam(required = false) String sellerId) {
        var data = voucherService.getAvailableVouchers(userId, sellerId);

        ApiResponse<?> response = ApiResponse.builder()
                .code(200)
                .message("Lấy danh sách voucher thành công")
                .result(data)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/getMyVoucherClaimedOfSeller")
    ApiResponse<List<UserVoucherResponse>> getMyVoucherClaimedOfSeller(
            @RequestParam String userId,
            @RequestParam String sellerId) {
        var data = voucherService.getMyVouchersBySeller(userId, sellerId);
        return ApiResponse.<List<UserVoucherResponse>>builder()
                .code(200)
                .message("Lấy voucher đã claim của người dùng thành công")
                .result(data)
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
     * Lấy lịch sử sử dụng voucher
     * GET /api/vouchers/usage-history?userId=xxx hoặc ?voucherId=yyy
     */
    @GetMapping("/usage-history")
    public ResponseEntity<ApiResponse<?>> getUsageHistory(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String voucherId) {
//        var data = voucherService.getUsageHistory(userId, voucherId);

        ApiResponse<?> response = ApiResponse.builder()
                .code(200)
                .message("Lấy lịch sử sử dụng voucher thành công")
//                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Admin/Seller tạo voucher
     * POST /api/vouchers
     */
    @PostMapping
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
    @PutMapping("/{id}")
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
    @DeleteMapping("/{id}")
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
}