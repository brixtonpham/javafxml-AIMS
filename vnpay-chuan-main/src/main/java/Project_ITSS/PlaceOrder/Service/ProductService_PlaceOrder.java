package Project_ITSS.PlaceOrder.Service;


import Project_ITSS.PlaceOrder.Repository.ProductRepository_PlaceOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductService_PlaceOrder {

    @Autowired
    private ProductRepository_PlaceOrder productRepository;

    public boolean checkProductValidity(int quantity,long product_id){
        int available_quantity = productRepository.getProductQuantity(product_id);
        if (quantity > available_quantity) return false;
        else return true;
    }


}
