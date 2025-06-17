package Project_ITSS.PlaceOrder.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryInfo {
    private String destination;
    private Date deliveryDate;
    private String customerId;
    private String customer;
    private boolean isPaid;
    private int totalPaid;
    private String qrInfo;
    private String productName;
    private int productQuantity;
    private String orderId;
    private float weight;

    public void createDeliveryInfo(String destination, Date deliveryDate, String customerId, String customer,
                                   boolean isPaid, int totalPaid, String qrInfo, String productName,
                                   int productQuantity, String orderId, float weight) {
        this.destination = destination;
        this.deliveryDate = deliveryDate;
        this.customerId = customerId;
        this.customer = customer;
        this.isPaid = isPaid;
        this.totalPaid = totalPaid;
        this.qrInfo = qrInfo;
        this.productName = productName;
        this.productQuantity = productQuantity;
        this.orderId = orderId;
        this.weight = weight;
    }
} 