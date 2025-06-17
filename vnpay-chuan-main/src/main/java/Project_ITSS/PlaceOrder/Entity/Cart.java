package Project_ITSS.PlaceOrder.Entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Cart {
     private List<CartItem> listofProducts = new ArrayList<>();


     public void addProducts(List<CartItem> products){
         for (CartItem Cartproduct : products){
             listofProducts.add(Cartproduct);
         }
     }

    public List<CartItem> getProducts(){
         return listofProducts;
     }

     public void EmptyCart(){
         listofProducts = new ArrayList<CartItem>();
     }


//    public boolean checkTheValidity(){
//         for(CartItem Cartproduct  : listofProducts){
//             Product product = Cartproduct.getProduct();
//             int quantity = Cartproduct.getQuantity();
//             if(!product.checkProductValidity(quantity,product.getProduct_id())){
//                 return false;
//             }
//
//         }
//         return true;
//     }


}
