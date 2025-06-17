package Project_ITSS.UpdateProduct.Service;


import Project_ITSS.AddProduct.Repository.DetailProductRepository_AddProduct;
import Project_ITSS.UpdateProduct.Entity.Product;
import Project_ITSS.UpdateProduct.Repository.DetailProductRepository_UpdateProduct;
import Project_ITSS.UpdateProduct.Repository.ProductRepository_UpdateProduct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductService_UpdateProduct {
    private final Map<String, DetailProductRepository_UpdateProduct> repositoryMap;
    @Autowired
    public ProductService_UpdateProduct(List<DetailProductRepository_UpdateProduct> repositories) {
        repositoryMap = new HashMap<>();
        for (DetailProductRepository_UpdateProduct repo : repositories) {
            repositoryMap.put(repo.getType(), repo);
        }
    }
    @Autowired
    private ProductRepository_UpdateProduct productRepository;

    public void updateProductInfo(Product product){
        productRepository.updateProductInfo(product);
    }

    public void updateProductDetail(Product product,String type){
        DetailProductRepository_UpdateProduct repo = repositoryMap.get(type);
        repo.updateProductInfo(product);
    }

}
