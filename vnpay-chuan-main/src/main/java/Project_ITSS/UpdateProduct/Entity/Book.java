package Project_ITSS.UpdateProduct.Entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Book extends Product {
    private long Book_id;
    private String genre;
    private int page_count;
    private String publication_date;
    private String authors;
    private String publishers;
    private String coverType;
}
