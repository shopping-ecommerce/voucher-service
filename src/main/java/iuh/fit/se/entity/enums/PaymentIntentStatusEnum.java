 package iuh.fit.se.entity.enums;

public enum PaymentIntentStatusEnum {
    PENDING,    // Đang chờ thanh toán
    COMPLETED,  // Thanh toán thành công, đã tạo Order
    FAILED,     // Thanh toán thất bại (từ VNPay trả về)
    EXPIRED     // Hết hạn chờ, đã được rollback
}