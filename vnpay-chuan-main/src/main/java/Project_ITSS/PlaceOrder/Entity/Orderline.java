package Project_ITSS.PlaceOrder.Entity;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.jdbc.core.JdbcTemplate;

@Getter
@Setter
@NoArgsConstructor
public class Orderline {
    private static int globalOdrline_id = 1;
    private long odrline_id;
    private long order_id;
    private long product_id;
    private String status;
    private boolean Rush_order;
    private int quantity;
    private int total_Fee;
    private String delivery_time;
    private String instructions;

    private JdbcTemplate jdbcTemplate;

    public void createOrderline(long order_id,long product_id,int quantity,int price){
         this.order_id = order_id;
         this.odrline_id = globalOdrline_id++;
         this.total_Fee = quantity * price;
         this.quantity = quantity;
         this.status = "pending";
         this.product_id = product_id;
    }


    public int getProductPrice(long product_id){
        return jdbcTemplate.queryForObject("SELECT price FROM Product WHERE product_id = ?", new Object[]{product_id}, Integer.class);
    }



}
