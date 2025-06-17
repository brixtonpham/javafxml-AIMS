package Project_ITSS.ViewProduct2.Entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
// import org.springframework.beans.factory.annotation.Autowired;

//import Project_ITSS.ViewProduct2.Repository.ProductRepository_ViewProduct;


@Getter
@Setter
@NoArgsConstructor
public  class Product {
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


//    private ProductRepository_ViewProduct productRepository = new ProductRepository_ViewProduct();
//    boolean checkProductValidity(int quantity,long product_id){
//        int available_quantity = productRepository.getProductQuantity(product_id);  // có thể là this.product_id
//        if (quantity > available_quantity) return false;
//        else return true;
//    }
}