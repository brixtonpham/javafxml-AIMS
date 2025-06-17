package Project_ITSS.PlaceOrder.Entity;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Product {
    private long product_id;
    private String title;
    private int price;
    private float weight;
    private boolean rush_order_supported;
    private String image_url;
    private String barcode;
    private String import_date;
    private String introduction;
    private int quantity;




}
