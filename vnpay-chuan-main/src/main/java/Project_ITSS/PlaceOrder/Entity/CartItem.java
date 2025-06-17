package Project_ITSS.PlaceOrder.Entity;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CartItem {
    Product product;
    int quantity;

    CartItem(Product product,int quantity){
        this.product = product;
        this.quantity = quantity;
    }

}
