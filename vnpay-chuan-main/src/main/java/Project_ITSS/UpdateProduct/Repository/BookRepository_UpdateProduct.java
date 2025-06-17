package Project_ITSS.UpdateProduct.Repository;


import Project_ITSS.UpdateProduct.Entity.Book;
import Project_ITSS.UpdateProduct.Entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public class BookRepository_UpdateProduct implements  DetailProductRepository_UpdateProduct{

    @Autowired
    private JdbcTemplate jdbcTemplate;


    public void updateProductInfo(Product product){
        Book book = (Book)product;
        String importDateStr = book.getPublication_date(); // ví dụ "2023-05-30"
        LocalDate localDate = LocalDate.parse(importDateStr);
        java.sql.Date sqlDate = java.sql.Date.valueOf(localDate);
        jdbcTemplate.update(
                "UPDATE Book " +               // <- dấu cách sau 'Book'
                        "SET " +                       // <- dấu cách sau 'SET'
                        "Product_id = ?, " +
                        "genre = ?, " +
                        "page_count = ?, " +
                        "publication_date = ?, " +
                        "authors = ?, " +
                        "publishers = ?, " +
                        "coverType = ? " +             // <- dấu cách quan trọng ở cuối
                        "WHERE Book_id = ?",
                book.getProduct_id(),
                book.getGenre(),
                book.getPage_count(),
                sqlDate,
                book.getAuthors(),
                book.getPublishers(),
                book.getCoverType(),
                book.getBook_id());
    }

    @Override
    public String getType() {
        return "book";
    }


}
