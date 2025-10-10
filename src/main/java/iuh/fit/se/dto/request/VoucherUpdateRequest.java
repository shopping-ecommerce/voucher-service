package iuh.fit.se.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherUpdateRequest {
    private String name;
    private String description;
    private Integer totalQuantity;
    private String status;
    private String endDate;
}
