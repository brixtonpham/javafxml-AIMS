package Project_ITSS.UpdateProduct.Entity;


import Project_ITSS.UpdateProduct.Entity.Book;
import Project_ITSS.UpdateProduct.Entity.CD;
import Project_ITSS.UpdateProduct.Entity.DVD;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Book.class, name = "book"),
        @JsonSubTypes.Type(value = CD.class, name = "cd"),
        @JsonSubTypes.Type(value = DVD.class, name = "dvd")
})
@Getter
@Setter
@NoArgsConstructor
public class Product {
    private long Product_id;
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
