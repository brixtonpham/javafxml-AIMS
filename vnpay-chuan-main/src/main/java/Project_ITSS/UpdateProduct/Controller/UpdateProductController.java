package Project_ITSS.UpdateProduct.Controller;


import Project_ITSS.UpdateProduct.Entity.Book;
import Project_ITSS.UpdateProduct.Entity.CD;
import Project_ITSS.UpdateProduct.Entity.DVD;
import Project_ITSS.UpdateProduct.Entity.Product;
import Project_ITSS.UpdateProduct.Repository.BookRepository_UpdateProduct;
import Project_ITSS.UpdateProduct.Repository.CDRepository_UpdateProduct;
import Project_ITSS.UpdateProduct.Repository.DVDRepository_UpdateProduct;
import Project_ITSS.UpdateProduct.Repository.ProductRepository_UpdateProduct;
import Project_ITSS.UpdateProduct.Service.ProductService_UpdateProduct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

class ProductRequest {
    private Project_ITSS.UpdateProduct.Entity.Product product;
    private String type;

    // Getters và setters
    public Project_ITSS.UpdateProduct.Entity.Product getProduct() { return product; }
    public void setProduct(Project_ITSS.UpdateProduct.Entity.Product product) { this.product = product; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}

@RestController
public class UpdateProductController {

   @Autowired
   private ProductRepository_UpdateProduct productRepository;
   @Autowired
   private BookRepository_UpdateProduct bookRepository;
   @Autowired
   private CDRepository_UpdateProduct cdRepository;
   @Autowired
   private DVDRepository_UpdateProduct dvdRepository;
   @Autowired
   private ProductService_UpdateProduct productService;


   // Khi người dùng muốn Add hoặc Update một product, hàm này sẽ trả về các giao diện để điền thông tin
    // Vì chưa có FE nên đoạn này để tạm như ở dưới
   @GetMapping("/UpdatingRequested")
   public String requestToUpdateProduct(){
        return "yes";
   }


   // Kiểm tra tính hợp lệ của thông tin đã được đưa vào
   public boolean checkProductInfoValidity(Product product){
       if(product.getTitle().length() > 100 || product.getImage_url().length() > 100 || product.getBarcode().length() > 100 || product.getImport_date().length() > 100 ||  product.getIntroduction().length() > 100) return false;
       DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

       if(product instanceof Book book){
           try{
                LocalDate.parse(book.getPublication_date(), formatter);
            }catch (DateTimeParseException e){
                 return false;
            }
       }else if(product instanceof DVD dvd){
           try{
                 LocalDate.parse(dvd.getImport_date(), formatter);
             }catch (DateTimeParseException e){
                 return false;
             }
       }else if(product instanceof CD cd){
            try{
                LocalDate.parse(cd.getImport_date(), formatter);
                LocalDate.parse(cd.getReleaseDate(), formatter);
            }catch (DateTimeParseException e){
                return false;
            }
       }else{
           return false;
       }
       return true;
   }


   @PostMapping("/updating/ProductInfo")
   public String UpdateProductInfo(@RequestBody ProductRequest productRequest){
//       if(!checkProductInfoValidity(product)){
//           return "Not OK";
//       }
       // Lưu lại các thông tin đó vào database
       productService.updateProductInfo(productRequest.getProduct());
       // Lưu lại các thông tin đó vào database, dựa trên loại của product đó
       productService.updateProductDetail(productRequest.getProduct(),productRequest.getType());
       return "OK";
   }

}
