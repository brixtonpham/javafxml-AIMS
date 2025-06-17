package Project_ITSS.ViewProduct2.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import Project_ITSS.ViewProduct2.Entity.Product;
import Project_ITSS.ViewProduct2.Repository.ProductRepository_ViewProduct;

import java.util.List;


@Service
public class ProductService_ViewProduct {
    @Autowired
    private ProductRepository_ViewProduct productRepository;



    public boolean checkProductValidity(int quantity,long product_id){
        int available_quantity = productRepository.getProductQuantity(product_id);
        if (quantity > available_quantity) return false;
        else return true;
    }

    public Product getBasicProductDetail(long id) {
        Product product = productRepository.findById(id);
        // Xoá/ẩn thông tin nhạy cảm nếu cần
        // product.setBarcode(null);
        product.setImport_date(null);
        product.setQuantity(0); /// không cho xem số lượng còn lại, chỉ cho xem available hay khong
        
        return product;
    }

    public Product getFullProductDetail(long id) {
        return productRepository.findById(id);
    }

    public List<Product> getAllProduct(){
//        productRepository = new ProductRepository_ViewProduct();
        return productRepository.getAllProduct();
    }
}
