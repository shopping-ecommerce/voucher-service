package iuh.fit.se.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class VoucherCreateRequest {
    private String code;
    private String name;
    private String description;
    private String type;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private Integer totalQuantity;
    private String startDate;
    private String endDate;
    private String applicableTo;
    private List<String> applicableIds;
    private String createdBy;
}