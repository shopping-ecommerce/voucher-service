package iuh.fit.se.controller;

import iuh.fit.event.dto.OrderStatusChangedEvent;
import iuh.fit.se.service.VoucherService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.RestController;

@RestController
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@RequiredArgsConstructor
public class NotificationController {
    VoucherService voucherService;

    @KafkaListener(topics = "order-updated", groupId = "voucher-service-group", concurrency = "1")
    public void handleOrderStatusChangedEvent(OrderStatusChangedEvent event) {
        log.info("Nhận được sự kiện order-updated cho orderId: {}", event.getOrderId());
        try {
            voucherService.rollbackVoucher(event.getVoucherCode(),event.getOrderId());
            log.info("Đã xử lý hoàn voucher cho đơn hàng: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Lỗi khi hoàn voucher cho đơn hàng {}: {}", event.getOrderId(), e.getMessage());
        }
    }

    @KafkaListener(topics = "user-cancel-order", groupId = "voucher-service-group", concurrency = "1")
    public void handleUserCancelChangedEvent(OrderStatusChangedEvent event) {
        log.info("Nhận được sự kiện user-cancel-order cho orderId: {}", event.getOrderId());
        try {
            voucherService.rollbackVoucher(event.getVoucherCode(),event.getOrderId());
            log.info("Đã xử lý hoàn voucher cho đơn hàng: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Lỗi khi hoàn voucher cho đơn hàng {}: {}", event.getOrderId(), e.getMessage());
        }
    }
}
