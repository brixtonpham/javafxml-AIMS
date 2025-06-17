package Project_ITSS.PlaceOrder.Entity;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Order {
    private static int globalOrderId = 1;
    private long order_id;
    private int Total_before_VAT;
    private int Total_after_VAT;
    private String status;
    private final int VAT = 10;
    List<Orderline> orderlineList = new ArrayList<>();


    public void createOrder(Cart cart){
        this.order_id = globalOrderId++;
        for(CartItem Cartproduct  : cart.getProducts()){
            Product product = Cartproduct.getProduct();
            int quantity = Cartproduct.getQuantity();
            this.Total_before_VAT += quantity * product.getPrice();
            this.Total_after_VAT += (quantity * product.getPrice()) + ((quantity * product.getPrice()) * this.VAT)/100;
            Orderline orderline = new Orderline();
            this.setStatus("pending");
            System.out.println(this.order_id);
            orderline.createOrderline(this.order_id,product.getProduct_id(),quantity,product.getPrice());
            orderlineList.add(orderline);
        }
    }

    public List<Orderline> getOrderLineList(){
        return orderlineList;
    }

    public void saveOrder(Order order,DeliveryInformation deliveryInformation){

    }




}
