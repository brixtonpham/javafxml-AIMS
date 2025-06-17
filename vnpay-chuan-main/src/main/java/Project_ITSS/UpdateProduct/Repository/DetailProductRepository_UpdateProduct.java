package Project_ITSS.UpdateProduct.Repository;

import Project_ITSS.UpdateProduct.Entity.Product;

public interface DetailProductRepository_UpdateProduct {
    public void updateProductInfo(Product product);
    public String getType();
}
