package iuh.fit.se.service.impl;

import iuh.fit.se.dto.request.*;
import iuh.fit.se.dto.response.*;
import iuh.fit.se.entity.*;
import iuh.fit.se.entity.enums.*;
import iuh.fit.se.exception.AppException;
import iuh.fit.se.exception.ErrorCode;
import iuh.fit.se.repository.*;
import iuh.fit.se.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * User claim voucher - CHỈ ĐƯỢC CLAIM 1 LẦN
     */
    @Transactional
    public void claimVoucher(String voucherCode, String userId) {
        log.info("User {} attempting to claim voucher {}", userId, voucherCode);

        // 1. Tìm voucher với pessimistic lock để tránh race condition
        Voucher voucher = voucherRepository.findByCodeWithLock(voucherCode)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 2. Kiểm tra voucher còn hiệu lực
        if (!voucher.isValid()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // 3. Kiểm tra user đã claim voucher này chưa - ĐÂY LÀ ĐIỂM QUAN TRỌNG
        boolean alreadyClaimed = userVoucherRepository
                .existsByUserIdAndVoucherId(userId, voucher.getId());

        if (alreadyClaimed) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        // 4. Kiểm tra số lượng voucher còn lại
        if (!voucher.hasQuantityLeft()) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        // 5. Tạo record UserVoucher
        UserVoucher userVoucher = UserVoucher.builder()
                .userId(userId)
                .voucherId(voucher.getId())
                .build();

        userVoucherRepository.save(userVoucher);

        // 6. Tăng claimedQuantity
        voucher.setClaimedQuantity(voucher.getClaimedQuantity() + 1);
        voucherRepository.save(voucher);

        log.info("User {} claimed voucher {} successfully", userId, voucherCode);
    }

    /**
     * Validate voucher trước khi user dùng
     */
    @Transactional(readOnly = true)
    public VoucherValidationResponse validateVoucher(VoucherValidationRequest request) {
        log.info("Validating voucher {} for user {}", request.getVoucherCode(), request.getUserId());

        // 1. Kiểm tra user có voucher này không
        UserVoucher userVoucher = userVoucherRepository
                .findByUserIdAndVoucherCode(request.getUserId(), request.getVoucherCode())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 2. Kiểm tra voucher có thể dùng không
        if (!userVoucher.canBeUsed()) {
            String statusMessage = switch (userVoucher.getStatus()) {
                case USED -> "Voucher đã được sử dụng rồi";
                case EXPIRED -> "Voucher đã hết hạn";
                default -> "Voucher không thể sử dụng";
            };
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        // 3. Lấy thông tin voucher
        Voucher voucher = voucherRepository.findById(userVoucher.getVoucherId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 4. Kiểm tra voucher còn hiệu lực
        if (!voucher.isValid()) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        // 5. Validate điều kiện áp dụng
        validateVoucherConditions(voucher, request);

        // 6. Tính toán discount
        BigDecimal discountAmount = calculateDiscount(voucher, request);

        // 7. Return response
        return VoucherValidationResponse.builder()
                .valid(true)
                .message("Áp dụng voucher thành công")
                .voucherInfo(VoucherValidationResponse.VoucherInfo.builder()
                        .voucherId(voucher.getId())
                        .code(voucher.getCode())
                        .name(voucher.getName())
                        .discountAmount(discountAmount)
                        .finalAmount(request.getOrderAmount().subtract(discountAmount))
                        .build())
                .build();
    }

    /**
     * Apply voucher khi order thành công - CHỈ DÙNG ĐƯỢC 1 LẦN
     */
    @Transactional
    public void applyVoucher(ApplyVoucherRequest request) {
        log.info("Applying voucher {} for order {}", request.getVoucherId(), request.getOrderId());

        // 1. Tìm UserVoucher
        UserVoucher userVoucher = userVoucherRepository
                .findByUserIdAndVoucherId(request.getUserId(), request.getVoucherId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 2. Kiểm tra voucher có thể dùng không
        if (!userVoucher.canBeUsed()) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        // 3. Mark voucher as USED - ĐÂY LÀ ĐIỂM QUAN TRỌNG
        userVoucher.markAsUsed(request.getOrderId());
        userVoucherRepository.save(userVoucher);

        // 4. Lấy thông tin voucher
        Voucher voucher = voucherRepository.findById(request.getVoucherId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 5. Tạo VoucherUsage record
        VoucherUsage usage = VoucherUsage.builder()
                .voucherId(request.getVoucherId())
                .voucherCode(voucher.getCode())
                .userId(request.getUserId())
                .orderId(request.getOrderId())
                .discountAmount(request.getDiscountAmount())
                .orderAmount(request.getOrderAmount())
                .build();

        voucherUsageRepository.save(usage);

        // 6. Tăng usedQuantity
        voucher.setUsedQuantity(voucher.getUsedQuantity() + 1);
        voucherRepository.save(voucher);

        log.info("Applied voucher {} for order {} successfully", request.getVoucherId(), request.getOrderId());
    }

    /**
     * Rollback voucher khi order bị hủy
     */
    @Transactional
    public void rollbackVoucher(String orderId) {
        log.info("Rolling back voucher for order {}", orderId);

        // 1. Tìm VoucherUsage
        VoucherUsage usage = voucherUsageRepository.findByOrderId(orderId)
                .orElse(null);

        if (usage == null) {
            log.info("No voucher usage found for order {}", orderId);
            return;
        }

        // 2. Tìm UserVoucher và mark lại thành CLAIMED
        UserVoucher userVoucher = userVoucherRepository
                .findByUserIdAndVoucherId(usage.getUserId(), usage.getVoucherId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        userVoucher.setStatus(UserVoucherStatusEnum.CLAIMED);
        userVoucher.setUsedTime(null);
        userVoucher.setOrderId(null);
        userVoucherRepository.save(userVoucher);

        // 3. Giảm usedQuantity
        Voucher voucher = voucherRepository.findById(usage.getVoucherId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        voucher.setUsedQuantity(Math.max(0, voucher.getUsedQuantity() - 1));
        voucherRepository.save(voucher);

        // 4. Xóa VoucherUsage record
        voucherUsageRepository.delete(usage);

        log.info("Rolled back voucher for order {} successfully", orderId);
    }

    /**
     * Lấy danh sách voucher available (chưa claim)
     */
    @Transactional(readOnly = true)
    public List<VoucherResponse> getAvailableVouchers(String userId, String sellerId) {
        log.info("Getting available vouchers for user {}", userId);

        LocalDateTime now = LocalDateTime.now();
        List<Voucher> vouchers;

        if (sellerId != null && !sellerId.isEmpty()) {
            vouchers = voucherRepository.findAvailableVouchersBySeller(now, sellerId);
        } else {
            vouchers = voucherRepository.findAvailableVouchers(now);
        }

        // Lọc ra những voucher user chưa claim
        return vouchers.stream()
                .filter(v -> !userVoucherRepository.existsByUserIdAndVoucherId(userId, v.getId()))
                .map(this::toVoucherResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách voucher của user (đã claim)
     */
    @Transactional(readOnly = true)
    public List<UserVoucherResponse> getMyVouchers(String userId) {
        log.info("Getting vouchers for user {}", userId);

        List<UserVoucher> userVouchers = userVoucherRepository
                .findByUserIdOrderByClaimedTimeDesc(userId);

        return userVouchers.stream()
                .map(this::toUserVoucherResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserVoucherResponse> getMyVouchersBySeller(String userId, String sellerId) {
        log.info("Getting vouchers for user {} by seller {}", userId, sellerId);
        LocalDateTime now = LocalDateTime.now();

        // Lấy tất cả (hoặc chỉ CLAIMED nếu bạn muốn)
        List<UserVoucher> userVouchers = userVoucherRepository.findByUserIdAndStatus(userId, UserVoucherStatusEnum.CLAIMED);


        return userVouchers.stream()
                .map(uv -> {
                    Voucher v = voucherRepository.findById(uv.getVoucherId()).orElse(null);
                    if (v == null) return null;

                    // 1) Voucher phải còn hiệu lực (ACTIVE, trong khoảng thời gian)
                    boolean validNow = v.getStatus() == VoucherStatusEnum.ACTIVE
                            && now.isAfter(v.getStartDate()) && now.isBefore(v.getEndDate());

                    // 2) Voucher phải "thuộc" seller này:
                    //    - Hoặc do seller đó tạo (createdBy == sellerId)
                    //    - Hoặc voucher áp dụng cho SPECIFIC_SELLERS và sellerId nằm trong applicableIds
                    //    - Hoặc voucher ALL (nếu bạn muốn ALL cũng dùng được cho mọi seller)
                    boolean belongToSeller =
                            sellerId == null || sellerId.isBlank()
                                    || v.getCreatedBy().equals(sellerId); // tuỳ business, có thể bỏ dòng này

                    // 3) Nếu onlyUsable=true thì còn phải là voucher chưa dùng + còn hiệu lực
                    boolean usable = uv.getStatus() == UserVoucherStatusEnum.CLAIMED && validNow;

                    if (!belongToSeller) return null;
                    if (!usable) return null;

                    return toUserVoucherResponse(uv);
                })
                .filter(r -> r != null)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách voucher của seller với trạng thái của user
     * API này dùng cho trang seller page
     * CHỈ HIỂN THỊ VOUCHER CÒN HẠN
     */
    @Transactional(readOnly = true)
    public List<SellerVoucherResponse> getSellerVouchersForUser(String sellerId, String userId) {
        log.info("Getting seller {} vouchers for user {}", sellerId, userId);

        LocalDateTime now = LocalDateTime.now();

        // 1. Lấy voucher của seller CÒN HẠN và ACTIVE
        List<Voucher> vouchers = voucherRepository.findByCreatedBy(sellerId).stream()
                .filter(v -> v.getStatus() == VoucherStatusEnum.ACTIVE
                        && now.isAfter(v.getStartDate())
                        && now.isBefore(v.getEndDate()))
                .collect(Collectors.toList());

        // 2. Lấy danh sách UserVoucher của user này
        List<UserVoucher> userVouchers = userVoucherRepository
                .findByUserIdOrderByClaimedTimeDesc(userId);

        // Tạo map để tra cứu nhanh
        Map<String, UserVoucher> userVoucherMap = userVouchers.stream()
                .collect(Collectors.toMap(
                        UserVoucher::getVoucherId,
                        uv -> uv,
                        (existing, replacement) -> existing
                ));

        // 3. Map sang SellerVoucherResponse
        return vouchers.stream()
                .map(voucher -> toSellerVoucherResponse(voucher, userVoucherMap.get(voucher.getId()), now))
                .sorted((v1, v2) -> {
                    // Sắp xếp: chưa nhận trước, đã nhận sau
                    if (!v1.isUserClaimed() && v2.isUserClaimed()) return -1;
                    if (v1.isUserClaimed() && !v2.isUserClaimed()) return 1;
                    // Trong cùng nhóm, sắp xếp theo startDate giảm dần
                    return v2.getStartDate().compareTo(v1.getStartDate());
                })
                .collect(Collectors.toList());
    }


    /**
     * Lấy lịch sử sử dụng voucher
     */
    @Transactional(readOnly = true)
    public List<VoucherUsage> getUsageHistory(String userId, String voucherId) {
        if (userId != null) {
            return voucherUsageRepository.findByUserIdOrderByUsedTimeDesc(userId);
        } else if (voucherId != null) {
            return voucherUsageRepository.findByVoucherId(voucherId);
        }
        return List.of();
    }

    /**
     * Admin/Seller tạo voucher
     */
    @Transactional
    public VoucherResponse createVoucher(VoucherCreateRequest request) {
        log.info("Creating voucher with code {}", request.getCode());

        // Kiểm tra code đã tồn tại chưa
        if (voucherRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        Voucher voucher = Voucher.builder()
                .code(request.getCode().toUpperCase())
                .name(request.getName())
                .description(request.getDescription())
                .type(VoucherTypeEnum.valueOf(request.getType()))
                .discountValue(request.getDiscountValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .minOrderAmount(request.getMinOrderAmount())
                .totalQuantity(request.getTotalQuantity())
                .startDate(LocalDateTime.parse(request.getStartDate(), FORMATTER))
                .endDate(LocalDateTime.parse(request.getEndDate(), FORMATTER))
                .applicableTo(ApplicableToEnum.valueOf(request.getApplicableTo()))
                .applicableIds(request.getApplicableIds())
                .createdBy(request.getCreatedBy())
                .build();

        voucher = voucherRepository.save(voucher);

        log.info("Created voucher {} successfully", voucher.getId());
        return toVoucherResponse(voucher);
    }

    /**
     * Admin/Seller cập nhật voucher
     */
    @Transactional
    public VoucherResponse updateVoucher(String id, VoucherUpdateRequest request) {
        log.info("Updating voucher {}", id);

        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (request.getName() != null) {
            voucher.setName(request.getName());
        }
        if (request.getDescription() != null) {
            voucher.setDescription(request.getDescription());
        }
        if (request.getTotalQuantity() != null) {
            voucher.setTotalQuantity(request.getTotalQuantity());
        }
        if (request.getStatus() != null) {
            voucher.setStatus(VoucherStatusEnum.valueOf(request.getStatus()));
        }
        if (request.getEndDate() != null) {
            voucher.setEndDate(LocalDateTime.parse(request.getEndDate(), FORMATTER));
        }

        voucher = voucherRepository.save(voucher);

        log.info("Updated voucher {} successfully", id);
        return toVoucherResponse(voucher);
    }

    /**
     * Xóa voucher (soft delete)
     */
    @Transactional
    public void deleteVoucher(String id) {
        log.info("Deleting voucher {}", id);

        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        voucher.setStatus(VoucherStatusEnum.INACTIVE);
        voucherRepository.save(voucher);

        log.info("Deleted voucher {} successfully", id);
    }

    /**
     * Kiểm tra điều kiện áp dụng voucher
     */
    private void validateVoucherConditions(Voucher voucher, VoucherValidationRequest request) {
        // 1. Kiểm tra giá trị đơn hàng tối thiểu
        if (voucher.getMinOrderAmount() != null
                && request.getOrderAmount().compareTo(voucher.getMinOrderAmount()) < 0) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);}
    }


    /**
     * Tính toán discount amount
     */
    private BigDecimal calculateDiscount(Voucher voucher, VoucherValidationRequest request) {
        BigDecimal discount = BigDecimal.ZERO;

        switch (voucher.getType()) {
            case PERCENTAGE:
                // Tính discount theo %
                discount = request.getOrderAmount()
                        .multiply(voucher.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                // Apply max discount nếu có
                if (voucher.getMaxDiscountAmount() != null
                        && discount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                    discount = voucher.getMaxDiscountAmount();
                }
                break;

            case FIXED_AMOUNT:
                discount = voucher.getDiscountValue();
                // Không được giảm nhiều hơn giá trị đơn hàng
                if (discount.compareTo(request.getOrderAmount()) > 0) {
                    discount = request.getOrderAmount();
                }
                break;

            case FREE_SHIPPING:
                // Free shipping: discount = shipping fee
                discount = request.getShippingFee() != null
                        ? request.getShippingFee()
                        : BigDecimal.ZERO;
                break;
        }

        return discount;
    }

    /**
     * Convert Voucher to VoucherResponse
     */
    private VoucherResponse toVoucherResponse(Voucher voucher) {
        Integer remaining = null;
        if (voucher.getTotalQuantity() != null) {
            remaining = voucher.getTotalQuantity() - voucher.getClaimedQuantity();
        }

        return VoucherResponse.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .name(voucher.getName())
                .description(voucher.getDescription())
                .type(voucher.getType().name())
                .discountValue(voucher.getDiscountValue())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .minOrderAmount(voucher.getMinOrderAmount())
                .totalQuantity(voucher.getTotalQuantity())
                .claimedQuantity(voucher.getClaimedQuantity())
                .remainingQuantity(remaining)
                .startDate(voucher.getStartDate())
                .endDate(voucher.getEndDate())
                .applicableTo(voucher.getApplicableTo().name())
                .applicableIds(voucher.getApplicableIds())
                .status(voucher.getStatus().name())
                .build();
    }

    /**
     * Convert UserVoucher to UserVoucherResponse
     */
    private UserVoucherResponse toUserVoucherResponse(UserVoucher userVoucher) {
        Voucher voucher = voucherRepository.findById(userVoucher.getVoucherId())
                .orElse(null);

        if (voucher == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        boolean canUse = userVoucher.getStatus() == UserVoucherStatusEnum.CLAIMED
                && voucher.getStatus() == VoucherStatusEnum.ACTIVE
                && now.isAfter(voucher.getStartDate())
                && now.isBefore(voucher.getEndDate());

        return UserVoucherResponse.builder()
                .id(userVoucher.getId())
                .voucherId(voucher.getId())
                .code(voucher.getCode())
                .name(voucher.getName())
                .description(voucher.getDescription())
                .type(voucher.getType().name())
                .discountValue(voucher.getDiscountValue())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .minOrderAmount(voucher.getMinOrderAmount())
                .status(userVoucher.getStatus().name())
                .claimedTime(userVoucher.getClaimedTime())
                .usedTime(userVoucher.getUsedTime())
                .startDate(voucher.getStartDate())
                .endDate(voucher.getEndDate())
                .canUse(canUse)
                .build();
    }

    /**
     * Format tiền
     */
    private String formatMoney(BigDecimal amount) {
        return String.format("%,.0f đ", amount);
    }
    /**
     * Convert Voucher + UserVoucher sang SellerVoucherResponse
     */
    private SellerVoucherResponse toSellerVoucherResponse(
            Voucher voucher,
            UserVoucher userVoucher,
            LocalDateTime now) {

        Integer remaining = null;
        if (voucher.getTotalQuantity() != null) {
            remaining = voucher.getTotalQuantity() - voucher.getClaimedQuantity();
        }

        boolean userClaimed = userVoucher != null;
        String userVoucherStatus = userClaimed ? userVoucher.getStatus().name() : null;
        LocalDateTime userClaimedTime = userClaimed ? userVoucher.getClaimedTime() : null;
        LocalDateTime userUsedTime = userClaimed ? userVoucher.getUsedTime() : null;

        // Kiểm tra voucher còn hiệu lực
        boolean voucherValid = voucher.getStatus() == VoucherStatusEnum.ACTIVE
                && now.isAfter(voucher.getStartDate())
                && now.isBefore(voucher.getEndDate());

        // Kiểm tra còn số lượng
        boolean hasQuantity = remaining == null || remaining > 0;

        // User có thể claim không
        boolean canClaim = !userClaimed && voucherValid && hasQuantity;

        // User có thể dùng không
        boolean canUse = userClaimed
                && userVoucher.getStatus() == UserVoucherStatusEnum.CLAIMED
                && voucherValid;

        // Tạo message phù hợp
        String message = "";
        if (userClaimed) {
            switch (userVoucher.getStatus()) {
                case CLAIMED:
                    if (canUse) {
                        message = "Sẵn sàng sử dụng";
                    } else if (!voucherValid) {
                        message = "Voucher đã hết hạn";
                    } else {
                        message = "Đã nhận";
                    }
                    break;
                case USED:
                    message = "Đã sử dụng";
                    break;
                case EXPIRED:
                    message = "Đã hết hạn";
                    break;
            }
        } else {
            if (!voucherValid) {
                if (now.isBefore(voucher.getStartDate())) {
                    message = "Chưa đến thời gian";
                } else {
                    message = "Đã hết hạn";
                }
            } else if (!hasQuantity) {
                message = "Đã hết số lượng";
            } else {
                message = "Nhận ngay";
            }
        }

        return SellerVoucherResponse.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .name(voucher.getName())
                .description(voucher.getDescription())
                .type(voucher.getType().name())
                .discountValue(voucher.getDiscountValue())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .minOrderAmount(voucher.getMinOrderAmount())
                .totalQuantity(voucher.getTotalQuantity())
                .claimedQuantity(voucher.getClaimedQuantity())
                .remainingQuantity(remaining)
                .startDate(voucher.getStartDate())
                .endDate(voucher.getEndDate())
                .applicableTo(voucher.getApplicableTo().name())
                .applicableIds(voucher.getApplicableIds())
                .status(voucher.getStatus().name())
                .userClaimed(userClaimed)
                .userVoucherStatus(userVoucherStatus)
                .userClaimedTime(userClaimedTime)
                .userUsedTime(userUsedTime)
                .canClaim(canClaim)
                .canUse(canUse)
                .message(message)
                .build();
    }

    /**
     * Lấy danh sách voucher của user từ một seller cụ thể cho trang checkout.
     * Hàm sẽ trả về TẤT CẢ voucher user đã nhận của seller đó,
     * kèm theo trạng thái có thể sử dụng (canUse) là true hoặc false.
     *
     * @param userId      ID của người dùng
     * @param sellerId    ID của người bán
     * @param orderAmount Tổng giá trị đơn hàng hiện tại
     * @return Danh sách các voucher với cờ canUse được tính toán
     */
    @Transactional(readOnly = true)
    public List<UserVoucherResponse> getUsableVouchersForCheckout(String userId, String sellerId, BigDecimal orderAmount) {
        log.info("Getting all checkout vouchers for user {}, seller {}, orderAmount {}", userId, sellerId, orderAmount);

        // 1. Lấy tất cả voucher mà user đang ở trạng thái đã nhận (chưa sử dụng)
        List<UserVoucher> claimedVouchers = userVoucherRepository.findByUserIdAndStatus(userId, UserVoucherStatusEnum.CLAIMED);
        LocalDateTime now = LocalDateTime.now();

        return claimedVouchers.stream()
                .map(userVoucher -> {
                    // Lấy thông tin chi tiết của voucher gốc
                    Voucher voucher = voucherRepository.findById(userVoucher.getVoucherId()).orElse(null);
                    if (voucher == null || !voucher.getCreatedBy().equals(sellerId)) {
                        return null; // Bỏ qua nếu voucher không tồn tại hoặc không phải của seller này
                    }

                    // --- Tính toán trạng thái canUse ---

                    // Điều kiện 1: Voucher phải còn hiệu lực (active, trong hạn sử dụng)
                    boolean isValid = voucher.getStatus() == VoucherStatusEnum.ACTIVE
                            && now.isAfter(voucher.getStartDate())
                            && now.isBefore(voucher.getEndDate());

                    // Điều kiện 2: Đơn hàng phải đủ giá trị tối thiểu
                    boolean meetsMinOrderAmount = voucher.getMinOrderAmount() == null
                            || orderAmount.compareTo(voucher.getMinOrderAmount()) >= 0;

                    // Kết hợp tất cả điều kiện để ra được kết quả cuối cùng
                    boolean finalCanUse = isValid && meetsMinOrderAmount;

                    // --- Xây dựng đối tượng response ---
                    // Sử dụng lại hàm toUserVoucherResponse ban đầu của bạn và ghi đè giá trị canUse
                    // Hoặc xây dựng trực tiếp tại đây để rõ ràng
                    return UserVoucherResponse.builder()
                            .id(userVoucher.getId())
                            .voucherId(voucher.getId())
                            .code(voucher.getCode())
                            .name(voucher.getName())
                            .description(voucher.getDescription())
                            .type(voucher.getType().name())
                            .discountValue(voucher.getDiscountValue())
                            .maxDiscountAmount(voucher.getMaxDiscountAmount())
                            .minOrderAmount(voucher.getMinOrderAmount())
                            .status(userVoucher.getStatus().name())
                            .claimedTime(userVoucher.getClaimedTime())
                            .usedTime(userVoucher.getUsedTime())
                            .startDate(voucher.getStartDate())
                            .endDate(voucher.getEndDate())
                            .canUse(finalCanUse) // <-- Gán giá trị cuối cùng
                            .build();
                })
                .filter(response -> response != null) // Lọc bỏ các voucher không phải của seller
                .sorted(java.util.Comparator.comparing(UserVoucherResponse::isCanUse).reversed()) // Vẫn ưu tiên voucher dùng được lên đầu
                .collect(Collectors.toList());
    }
}