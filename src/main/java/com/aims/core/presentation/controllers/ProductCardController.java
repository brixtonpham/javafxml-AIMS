package com.aims.core.presentation.controllers;

import com.aims.core.entities.Product;
import com.aims.core.application.services.ICartService;
// import com.aims.presentation.utils.AlertHelper;
// import com.aims.presentation.utils.FXMLSceneManager; // For navigation
// import com.aims.MainLayoutController; // To navigate

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.sql.SQLException; // From ICartService
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.InventoryException;


public class ProductCardController {

    @FXML
    private VBox productCardVBox;
    @FXML
    private ImageView productImageView;
    @FXML
    private Label productTitleLabel;
    @FXML
    private Label productPriceLabel;
    @FXML
    private Label productAvailabilityLabel;
    @FXML
    private Button addToCartButton;

    private Product product;
    private ICartService cartService; // Sẽ được inject hoặc set từ controller cha (HomeScreenController)
    private MainLayoutController mainLayoutController; // Để điều hướng khi click vào card
    // private FXMLSceneManager sceneManager;

    // Biến để tham chiếu đến controller cha nếu cần gọi lại (ví dụ: refresh home screen sau khi thêm vào giỏ)
    private HomeScreenController homeScreenController;


    public ProductCardController() {
        // Constructor - services will be injected or set
    }

    /**
     * Setter cho CartService, được gọi bởi controller cha (HomeScreenController).
     */
    public void setCartService(ICartService cartService) {
        this.cartService = cartService;
    }

    /**
     * Setter cho MainLayoutController, được gọi bởi controller cha.
     */
    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }

    /**
     * Setter cho HomeScreenController, để có thể gọi lại các phương thức của nó nếu cần.
     */
    public void setHomeScreenController(HomeScreenController homeScreenController) {
        this.homeScreenController = homeScreenController;
    }

    /**
     * Điền dữ liệu sản phẩm vào card.
     * @param product Đối tượng Product (đã bao gồm giá có VAT nếu hiển thị cho khách hàng).
     */
    public void setData(Product product) {
        this.product = product;
        if (product == null) {
            // Xử lý trường hợp product là null, ví dụ ẩn card hoặc hiển thị placeholder
            productCardVBox.setVisible(false);
            productCardVBox.setManaged(false);
            return;
        }
        productCardVBox.setVisible(true);
        productCardVBox.setManaged(true);

        productTitleLabel.setText(product.getTitle());
        // Giá hiển thị trên card nên là giá đã bao gồm VAT.
        // ProductService.getProductsForDisplay() nên trả về sản phẩm với giá đã tính VAT.
        productPriceLabel.setText(String.format("%,.0f VND", product.getPrice()));
        productAvailabilityLabel.setText("Available: " + product.getQuantityInStock());

        updateAddToCartButtonState();

        // Load image
        if (product.getImageUrl() != null && !product.getImageUrl().trim().isEmpty()) {
            try {
                // Giả sử getImageUrl() trả về một URL hợp lệ hoặc đường dẫn file resource
                // Nếu là URL từ web: new Image(product.getImageUrl(), true) // true for background loading
                // Nếu là resource trong project: new Image(getClass().getResourceAsStream(product.getImageUrl()))
                Image image = new Image(product.getImageUrl(), true); // true for background loading
                productImageView.setImage(image);
            } catch (Exception e) {
                System.err.println("Error loading image for product card (" + product.getTitle() + "): " + product.getImageUrl() + " - " + e.getMessage());
                loadPlaceholderImage();
            }
        } else {
            loadPlaceholderImage();
        }
    }

    private void loadPlaceholderImage() {
        try {
            // Image placeholder = new Image(getClass().getResourceAsStream("/assets/images/product_placeholder.png"));
            // productImageView.setImage(placeholder);
            // System.out.println("Loaded placeholder image for product card.");
        } catch (Exception e) {
            System.err.println("Error loading placeholder image: " + e.getMessage());
        }
    }

    private void updateAddToCartButtonState() {
        if (product.getQuantityInStock() <= 0) {
            addToCartButton.setText("Out of Stock");
            addToCartButton.setDisable(true);
            productAvailabilityLabel.setText("Out of Stock");
            productAvailabilityLabel.setStyle("-fx-text-fill: red;");
        } else {
            addToCartButton.setText("Add to Cart");
            addToCartButton.setDisable(false);
            productAvailabilityLabel.setText("Available: " + product.getQuantityInStock());
            productAvailabilityLabel.setStyle(""); // Reset style
        }
    }

    @FXML
    void handleAddToCartAction(ActionEvent event) {
        if (product == null) return;
        System.out.println("Add to cart clicked for (Card): " + product.getTitle());

        if (cartService == null) {
            System.err.println("CartService not available in ProductCardController.");
            // AlertHelper.showErrorAlert("Service Error", "Could not add to cart. Cart service is unavailable.");
            return;
        }

        try {
            // TODO: Lấy cartSessionId thực tế từ một nguồn quản lý session chung
            String currentCartSessionId = "guest_cart_session_id_placeholder"; // Placeholder
            cartService.addItemToCart(currentCartSessionId, product.getProductId(), 1); // Thêm 1 sản phẩm
            // AlertHelper.showInfoAlert("Added to Cart", product.getTitle() + " has been added to your cart.");

            // Cập nhật UI ngay lập tức (giả định thành công)
            // Service nên trả về thông tin cập nhật nếu cần, hoặc HomeScreenController nên load lại
            product.setQuantityInStock(product.getQuantityInStock() - 1); // Giảm tạm thời trên UI
            setData(this.product); // Cập nhật lại card để thay đổi nút và số lượng

            // Thông báo cho HomeScreenController để có thể cập nhật tổng giỏ hàng hoặc các thông tin khác nếu cần
            if (homeScreenController != null) {
                // homeScreenController.refreshCartSummary(); // Ví dụ
            }

        } catch (SQLException | ResourceNotFoundException | ValidationException | InventoryException e) {
            System.err.println("Failed to add product '" + product.getTitle() + "' to cart: " + e.getMessage());
            // AlertHelper.showErrorAlert("Add to Cart Failed", e.getMessage());
            // Có thể cần load lại thông tin sản phẩm để đảm bảo stock chính xác nếu có lỗi
             if(e instanceof InventoryException){
                productAvailabilityLabel.setText("Out of Stock!");
                productAvailabilityLabel.setStyle("-fx-text-fill: red;");
                addToCartButton.setText("Out of Stock");
                addToCartButton.setDisable(true);
            }
        }
        event.consume(); // Ngăn sự kiện click lan truyền lên VBox cha nếu không muốn
    }

    @FXML
    void handleViewProductDetails(MouseEvent event) {
        // Chỉ xử lý nếu click không phải vào nút "Add to Cart" (nếu nút nằm trong VBox)
        // Hoặc nếu muốn toàn bộ card là clickable để xem chi tiết
        if (event.getTarget() == addToCartButton || (addToCartButton.getGraphic() != null && event.getTarget() == addToCartButton.getGraphic())) {
            return; // Sự kiện đã được nút xử lý, không làm gì thêm
        }

        if (product == null) return;
        System.out.println("View details action for (Card): " + product.getTitle());

        // if (mainLayoutController != null && FXMLSceneManager.getInstance() != null && product != null) {
        //     try {
        //         FXMLLoader loader = FXMLSceneManager.getInstance().getLoader(FXMLSceneManager.PRODUCT_DETAIL_SCREEN);
        //         Parent productDetailNode = loader.load();
        //         ProductDetailScreenController detailController = loader.getController();
        //
        //         detailController.setMainLayoutController(mainLayoutController);
        //         detailController.setProductService(this.productService); // Assuming ProductCardController gets ProductService
        //         detailController.setCartService(this.cartService);
        //         detailController.setProductId(product.getProductId());
        //
        //         mainLayoutController.setContent(productDetailNode);
        //         mainLayoutController.setHeaderTitle("Product Details: " + product.getTitle());
        //
        //     } catch (IOException e) {
        //         e.printStackTrace();
        //         AlertHelper.showErrorAlert("Navigation Error", "Could not load product details screen.");
        //     }
        // }
    }
}