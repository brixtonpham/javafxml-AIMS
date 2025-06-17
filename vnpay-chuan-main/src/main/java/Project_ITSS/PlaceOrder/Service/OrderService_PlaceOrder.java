package Project_ITSS.PlaceOrder.Service;

import Project_ITSS.PlaceOrder.Entity.DeliveryInfo;
import Project_ITSS.PlaceOrder.Entity.DeliveryInformation;
import Project_ITSS.PlaceOrder.Entity.Order;
import Project_ITSS.PlaceOrder.Entity.Orderline;
import Project_ITSS.PlaceOrder.Repository.OrderRepository_PlaceOrder;
import Project_ITSS.PlaceOrder.Repository.ProductRepository_PlaceOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService_PlaceOrder {
    @Autowired
    private OrderRepository_PlaceOrder orderRepository;
    @Autowired
    private ProductRepository_PlaceOrder productRepository;
    public void saveRushOrder(DeliveryInfo deliveryInfo) {
        // TODO: Lưu thông tin rush order vào DB hoặc xử lý logic liên quan
        System.out.println("Rush order saved: " + deliveryInfo);
    }

    public void saveOrder(Order order, DeliveryInformation deliveryInfo){
        orderRepository.saveOrder(order,deliveryInfo);
    }

    public int[] CalculateDeliveryFee(String province,Order order){
        List<Orderline> orderlineList = order.getOrderLineList();
        int normal_delivery_fee = 0;
        int rush_delivery_fee = 0;
        if(province.equals("Hanoi") || province.equals("HoChiMinhCity")){
            for (Orderline orderline : orderlineList){
                double weight = productRepository.getProductWeight(orderline.getProduct_id());
                int total = 0;
                total += 22000;
                weight = (weight - 3);
                while(weight > 0.5){
                    total += 2500;
                    weight -= 0.5;
                }
                if(orderline.isRush_order()){
                    rush_delivery_fee += total + 10000;
                }else{
                    normal_delivery_fee += total;
                }
            }
            normal_delivery_fee = (normal_delivery_fee > 25000) ? normal_delivery_fee - 25000 : 0;
        }else{
            for (Orderline orderline : orderlineList){
                double weight = productRepository.getProductWeight(orderline.getProduct_id());
                int total = 0;
                total += 30000;
                weight = (weight - 0.5);
                while(weight > 0.5){
                    total += 2500;
                    weight -= 0.5;
                }
                if(orderline.isRush_order()){
                    rush_delivery_fee += total + 10000;
                }else{
                    normal_delivery_fee += total;
                }
            }
        }
        if(order.getTotal_before_VAT() > 100000){
            normal_delivery_fee = (normal_delivery_fee > 25000) ? normal_delivery_fee - 25000 : 0;
        }
        int[] delivery_fees = {normal_delivery_fee,rush_delivery_fee};
        return delivery_fees;
    }
    // ... các method khác cho order thường
} 