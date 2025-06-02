package com.aims.presentation.controllers;

import com.aims.core.application.dtos.CartItemDTO;
import com.aims.core.application.dtos.CartViewDTO; // Để nhận dữ liệu từ service
import com.aims.core.application.services.ICartService;
// import com.aims.presentation.utils.AlertHelper;
// import com.aims.presentation.utils.FXMLSceneManager;


import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;


import java.sql.SQLException;

public class CartScreenController {

    @FXML
    private TableView<CartItemDTO> cartTableView;
    @FXML
    private TableColumn<CartItemDTO, ImageView> productImageColumn;
    @FXML
    private TableColumn<CartItemDTO, String> productTitleColumn;
    @FXML
    private TableColumn<CartItemDTO, Float> unitPriceColumn;
    @FXML
    private TableColumn<CartItemDTO, Integer> quantityColumn; // Sẽ custom cell
    @FXML
    private TableColumn<CartItemDTO, Float> totalItemPriceColumn;
    @FXML
    private TableColumn<CartItemDTO, Void> actionsColumn;


    @FXML
    private Label totalCartPriceLabel;
    @FXML
    private Button checkoutButton;
    @FXML
    private VBox stockWarningVBox;


    // @Inject
    private ICartService cartService;
    // private MainLayoutController mainLayoutController;
    // private FXMLSceneManager sceneManager;


    private ObservableList<CartItemDTO> cartItemsList = FXCollections.observableArrayList();
    private String currentCartSessionId = "guest_cart_session"; // TODO: Lấy session ID thực tế


    public CartScreenController() {
        // cartService = new CartServiceImpl(...); // DI
    }


    // public void setMainLayoutController(MainLayoutController mainLayoutController) { this.mainLayoutController = mainLayoutController; }
    // public void setCartService(ICartService cartService) { this.cartService = cartService; }


    public void initialize() {
        // sceneManager = FXMLSceneManager.getInstance();
        setupTableColumns();
        loadCartDetails();
    }


    private void setupTableColumns() {
        productImageColumn.setCellValueFactory(param -> {
            ImageView imageView = new ImageView();
            imageView.setFitHeight(50);
            imageView.setFitWidth(50);
            imageView.setPreserveRatio(true);
            if (param.getValue().getImageUrl() != null && !param.getValue().getImageUrl().isEmpty()) {
                try {
                    imageView.setImage(new Image(param.getValue().getImageUrl(), true));
                } catch (Exception e) { /* placeholder or default image */ }
            }
            return new SimpleObjectProperty<>(imageView);
        });


        productTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        unitPriceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPriceExclVAT"));
        unitPriceColumn.setCellFactory(tc -> new TableCell<CartItemDTO, Float>() {
            @Override
            protected void updateItem(Float price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.0f VND", price));
                }
            }
        });


        totalItemPriceColumn.setCellValueFactory(new PropertyValueFactory<>("totalPriceExclVAT"));
        totalItemPriceColumn.setCellFactory(tc -> new TableCell<CartItemDTO, Float>() {
            @Override
            protected void updateItem(Float price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.0f VND", price));
                }
            }
        });


        // Custom cell for quantity with Spinner
        Callback<TableColumn<CartItemDTO, Integer>, TableCell<CartItemDTO, Integer>> quantityCellFactory =
            param -> new QuantityCell();
        quantityColumn.setCellFactory(quantityCellFactory);
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));


        // Custom cell for action buttons
        Callback<TableColumn<CartItemDTO, Void>, TableCell<CartItemDTO, Void>> actionCellFactory =
            param -> new ActionCell();
        actionsColumn.setCellFactory(actionCellFactory);
    }


    private void loadCartDetails() {
        // if (cartService == null) {
        //     AlertHelper.showErrorAlert("Error", "Cart service not available.");
        //     return;
        // }
        // try {
        //     // Cart entity trả về từ service nên chứa danh sách CartItem entity
        //     // Cần map sang CartItemDTO (hoặc CartViewDTO chứa list CartItemDTO)
        //     com.aims.core.entities.Cart cartEntity = cartService.getCart(currentCartSessionId); // This returns entity
        //
        //     if (cartEntity != null && cartEntity.getItems() != null) {
        //         cartItemsList.clear();
        //         float total = 0f;
        //         stockWarningVBox.getChildren().clear();
        //
        //         for (com.aims.core.entities.CartItem itemEntity : cartEntity.getItems()) {
        //             Product product = itemEntity.getProduct(); // Assuming product is loaded
        //             CartItemDTO dto = new CartItemDTO(
        //                     product.getProductId(),
        //                     product.getTitle(),
        //                     itemEntity.getQuantity(),
        //                     product.getPrice(), // Price ex VAT
        //                     product.getImageUrl(),
        //                     product.getQuantityInStock()
        //             );
        //             cartItemsList.add(dto);
        //             total += dto.getTotalPriceExclVAT();
        //
        //             if (!dto.isStockSufficient()) {
        //                 Label warning = new Label("Warning: Product '" + dto.getTitle() + "' only has " + dto.getAvailableStock() + " available. You requested " + dto.getQuantity() + ".");
        //                 warning.setStyle("-fx-text-fill: red;");
        //                 stockWarningVBox.getChildren().add(warning);
        //             }
        //         }
        //         cartTableView.setItems(cartItemsList);
        //         totalCartPriceLabel.setText(String.format("Total (excl. VAT): %,.0f VND", total));
        //         checkoutButton.setDisable(cartItemsList.isEmpty() || !stockWarningVBox.getChildren().isEmpty());
        //
        //     } else {
        //         cartTableView.getItems().clear();
        //         totalCartPriceLabel.setText("Total (excl. VAT): 0 VND");
        //         checkoutButton.setDisable(true);
        //         cartTableView.setPlaceholder(new Label("Your cart is currently empty."));
        //     }
        // } catch (SQLException e) {
        //     e.printStackTrace();
        //     AlertHelper.showErrorAlert("Database Error", "Could not load cart details: " + e.getMessage());
        // }
        // Dummy data for now as services are not injected
        System.out.println("loadCartDetails called - implement with actual service call and DTO mapping");
    }




    @FXML
    void handleClearCartAction(ActionEvent event) {
        // if (cartService == null) { AlertHelper.showErrorAlert("Error", "Service unavailable."); return; }
        // try {
        //     cartService.clearCart(currentCartSessionId);
        //     AlertHelper.showInfoAlert("Cart Cleared", "Your shopping cart has been emptied.");
        //     loadCartDetails(); // Refresh view
        // } catch (SQLException | ResourceNotFoundException e) {
        //     AlertHelper.showErrorAlert("Error Clearing Cart", e.getMessage());
        // }
         System.out.println("Clear Cart action - implement");
    }


    @FXML
    void handleProceedToCheckoutAction(ActionEvent event) {
        // if (cartItemsList.isEmpty()) {
        //     AlertHelper.showWarningAlert("Empty Cart", "Please add items to your cart before proceeding to checkout.");
        //     return;
        // }
        // if (!stockWarningVBox.getChildren().isEmpty()){
        //      AlertHelper.showErrorAlert("Stock Issue", "Please resolve stock issues before proceeding.");
        //      return;
        // }
        //
        // System.out.println("Proceed to Checkout action triggered");
        // if (sceneManager != null && mainLayoutController != null) {
        //     mainLayoutController.loadContent(FXMLSceneManager.DELIVERY_INFO_SCREEN);
        //     mainLayoutController.setHeaderTitle("Delivery Information");
        // }
         System.out.println("Proceed to Checkout action - implement navigation");
    }


    // Inner class for Quantity Spinner in TableCell
    private class QuantityCell extends TableCell<CartItemDTO, Integer> {
        private final Spinner<Integer> quantitySpinner;


        public QuantityCell() {
            quantitySpinner = new Spinner<>();
            SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 1); // Min 0 to allow removal by setting to 0
            quantitySpinner.setValueFactory(valueFactory);
            quantitySpinner.setEditable(true);
            quantitySpinner.valueProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue != null && getTableRow() != null && getTableRow().getItem() != null) {
                    CartItemDTO item = getTableView().getItems().get(getIndex());
                    if (item.getAvailableStock() < newValue && newValue > 0) { // Check against available stock for increase
                        // AlertHelper.showWarningAlert("Stock Limit", "Cannot exceed available stock: " + item.getAvailableStock());
                        quantitySpinner.getValueFactory().setValue(oldValue); // Revert
                        return;
                    }
                    if (newValue == 0) { // Handle removal if quantity is 0
                        handleRemoveItem(item);
                    } else if (newValue != oldValue ) { // Only update if value actually changed and not 0
                        handleUpdateQuantity(item, newValue);
                    }
                }
            });
            // Commit on focus lost - not ideal for immediate feedback, but simpler than custom editor
             quantitySpinner.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue) { // Lost focus
                    // The listener on valueProperty should have already handled the change.
                    // This can be a place for an explicit commit if needed for other spinner types.
                }
            });
        }


        @Override
        protected void updateItem(Integer itemQuantity, boolean empty) {
            super.updateItem(itemQuantity, empty);
            if (empty || itemQuantity == null) {
                setGraphic(null);
            } else {
                quantitySpinner.getValueFactory().setValue(itemQuantity);
                setGraphic(quantitySpinner);
            }
        }
    }


    // Inner class for Action Buttons in TableCell
    private class ActionCell extends TableCell<CartItemDTO, Void> {
        private final Button removeButton = new Button("Remove");


        public ActionCell() {
            removeButton.getStyleClass().add("button-danger-small");
            removeButton.setOnAction(event -> {
                CartItemDTO item = getTableView().getItems().get(getIndex());
                handleRemoveItem(item);
            });
        }


        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                HBox pane = new HBox(removeButton);
                pane.setSpacing(5);
                setGraphic(pane);
            }
        }
    }


    private void handleUpdateQuantity(CartItemDTO itemDto, int newQuantity) {
        // if (cartService == null) { AlertHelper.showErrorAlert("Error", "Service unavailable."); return; }
        // try {
        //     cartService.updateItemQuantity(currentCartSessionId, itemDto.getProductId(), newQuantity);
        //     loadCartDetails(); // Refresh the whole cart view
        // } catch (SQLException | ResourceNotFoundException | ValidationException | InventoryException e) {
        //     AlertHelper.showErrorAlert("Update Failed", e.getMessage());
        //     loadCartDetails(); // Revert UI by reloading
        // }
        System.out.println("Update quantity for " + itemDto.getProductId() + " to " + newQuantity + " - implement");
         loadCartDetails(); // Simulate refresh
    }


    private void handleRemoveItem(CartItemDTO itemDto) {
        // if (cartService == null) { AlertHelper.showErrorAlert("Error", "Service unavailable."); return; }
        // boolean confirmed = AlertHelper.showConfirmationDialog("Remove Item", "Are you sure you want to remove " + itemDto.getTitle() + " from your cart?");
        // if (confirmed) {
        //     try {
        //         cartService.removeItemFromCart(currentCartSessionId, itemDto.getProductId());
        //         loadCartDetails(); // Refresh the whole cart view
        //     } catch (SQLException | ResourceNotFoundException e) {
        //         AlertHelper.showErrorAlert("Remove Failed", e.getMessage());
        //         loadCartDetails(); // Revert UI by reloading
        //     }
        // }
         System.out.println("Remove item " + itemDto.getProductId() + " - implement");
         loadCartDetails(); // Simulate refresh
    }
}