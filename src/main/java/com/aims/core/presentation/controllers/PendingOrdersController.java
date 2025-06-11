package com.aims.core.presentation.controllers;

import com.aims.core.application.services.IOrderService;
import com.aims.core.entities.OrderEntity;
import com.aims.core.enums.OrderStatus;
import com.aims.core.presentation.utils.AlertHelper;
import com.aims.core.presentation.utils.FXMLSceneManager;
import com.aims.core.shared.utils.SearchResult; // Assuming SearchResult utility

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

public class PendingOrdersController {

    @FXML
    private TableView<OrderEntity> pendingOrdersTableView;
    @FXML
    private TableColumn<OrderEntity, String> orderIdColumn;
    @FXML
    private TableColumn<OrderEntity, String> orderDateColumn; // Will format LocalDateTime
    @FXML
    private TableColumn<OrderEntity, String> customerInfoColumn;
    @FXML
    private TableColumn<OrderEntity, Float> totalAmountColumn;
    @FXML
    private TableColumn<OrderEntity, Integer> itemCountColumn;
    @FXML
    private TableColumn<OrderEntity, Void> actionsColumn;

    @FXML
    private HBox paginationControls;
    @FXML
    private Button prevPageButton;
    @FXML
    private Label currentPageLabel;
    @FXML
    private Button nextPageButton;

    // @Inject
    private IOrderService orderService;
    private MainLayoutController mainLayoutController;
    private com.aims.core.presentation.utils.FXMLSceneManager sceneManager;

    private ObservableList<OrderEntity> pendingOrdersList = FXCollections.observableArrayList();
    private int currentPage = 1;
    private final int PAGE_SIZE = 30; // As per requirement
    private int totalPages = 1;
    private String currentManagerId; // TODO: Set this from login session

    public PendingOrdersController() {
        // orderService = new OrderServiceImpl(...); // DI
    }

    public void setMainLayoutController(MainLayoutController mainLayoutController) { this.mainLayoutController = mainLayoutController; }
    public void setSceneManager(com.aims.core.presentation.utils.FXMLSceneManager sceneManager) { this.sceneManager = sceneManager; }
    public void setOrderService(IOrderService orderService) { this.orderService = orderService; }
    public void setCurrentManagerId(String managerId) { this.currentManagerId = managerId; }


    public void initialize() {
        sceneManager = com.aims.core.presentation.utils.FXMLSceneManager.getInstance();
        setupTableColumns();
        loadPendingOrders();
    }

    private void setupTableColumns() {
        // orderIdColumn and totalAmountColumn are set via FXML PropertyValueFactory

        // Format Order Date
        orderDateColumn.setCellValueFactory(cellData -> {
            OrderEntity order = cellData.getValue();
            if (order != null && order.getOrderDate() != null) {
                return new SimpleStringProperty(order.getOrderDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }
            return new SimpleStringProperty("");
        });

        // Customer Info (Name or User ID)
        customerInfoColumn.setCellValueFactory(cellData -> {
            OrderEntity order = cellData.getValue();
            if (order != null) {
                if (order.getUserAccount() != null && order.getUserAccount().getUsername() != null) {
                    return new SimpleStringProperty(order.getUserAccount().getUsername() + " (User ID: " + order.getUserAccount().getUserId() + ")");
                } else if (order.getDeliveryInfo() != null && order.getDeliveryInfo().getRecipientName() != null) {
                    return new SimpleStringProperty(order.getDeliveryInfo().getRecipientName() + " (Guest)");
                }
            }
            return new SimpleStringProperty("N/A");
        });

        // Item Count
        itemCountColumn.setCellValueFactory(cellData -> {
            OrderEntity order = cellData.getValue();
            if (order != null && order.getOrderItems() != null) {
                return new SimpleIntegerProperty(order.getOrderItems().size()).asObject();
            }
            return new SimpleIntegerProperty(0).asObject();
        });

        // Format Total Amount
        totalAmountColumn.setCellFactory(tc -> new TableCell<OrderEntity, Float>() {
            @Override
            protected void updateItem(Float amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.0f VND", amount));
                }
            }
        });


        // Custom cell for action buttons (View/Review)
        Callback<TableColumn<OrderEntity, Void>, TableCell<OrderEntity, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<OrderEntity, Void> call(final TableColumn<OrderEntity, Void> param) {
                final TableCell<OrderEntity, Void> cell = new TableCell<>() {
                    private final Button reviewButton = new Button("Review");
                    private final HBox pane = new HBox(reviewButton);

                    {
                        reviewButton.getStyleClass().add("button-primary-small");
                        reviewButton.setOnAction((ActionEvent event) -> {
                            OrderEntity order = getTableView().getItems().get(getIndex());
                            handleReviewOrderAction(order);
                        });
                        pane.setSpacing(5);
                        pane.setAlignment(javafx.geometry.Pos.CENTER);
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(pane);
                        }
                    }
                };
                return cell;
            }
        };
        actionsColumn.setCellFactory(cellFactory);
        pendingOrdersTableView.setItems(pendingOrdersList);
    }

    private void loadPendingOrders() {
        if (orderService == null) {
            AlertHelper.showErrorDialog("Service Error", "Order Service Unavailable", "The order service is not properly initialized.");
            return;
        }
        try {
            SearchResult<OrderEntity> result = orderService.getOrdersByStatusForManager(
                    OrderStatus.PENDING_PROCESSING, currentPage, PAGE_SIZE
            );
            pendingOrdersList.setAll(result.results());
            updatePaginationControls(result.currentPage(), result.totalPages(), result.totalResults());

            if(result.results().isEmpty() && currentPage == 1){
                pendingOrdersTableView.setPlaceholder(new Label("No orders are currently pending processing."));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            AlertHelper.showErrorDialog("Database Error", "Failed to Load Orders", "Could not retrieve pending orders from the database: " + e.getMessage());
        }
        // System.out.println("loadPendingOrders called for page: " + currentPage + " - Implement with actual service call.");
        // Dummy data for testing UI
        // if (currentPage == 1) {
        //     OrderEntity o1 = new OrderEntity(); o1.setOrderId("ORD001"); o1.setOrderDate(LocalDateTime.now().minusHours(2)); o1.setTotalAmountPaid(150000f);
        //     OrderEntity o2 = new OrderEntity(); o2.setOrderId("ORD002"); o2.setOrderDate(LocalDateTime.now().minusHours(1)); o2.setTotalAmountPaid(250000f);
        //     pendingOrdersList.setAll(o1, o2);
        // } else {
        //     pendingOrdersList.clear();
        // }
        // updatePaginationControls(1, 1, pendingOrdersList.size());
    }

    private void updatePaginationControls(int current, int total, long totalItems) {
        this.currentPage = current;
        this.totalPages = total;
         if (totalItems == 0) {
            currentPageLabel.setText("No pending orders");
            paginationControls.setVisible(false);
        } else {
            currentPageLabel.setText("Page " + this.currentPage + " / " + this.totalPages);
            paginationControls.setVisible(true);
        }
        prevPageButton.setDisable(this.currentPage <= 1);
        nextPageButton.setDisable(this.currentPage >= this.totalPages);
    }

    @FXML
    void handleRefreshAction(ActionEvent event) {
        currentPage = 1; // Reset to first page
        loadPendingOrders();
    }

    private void handleReviewOrderAction(OrderEntity order) {
        System.out.println("Review action for order: " + order.getOrderId());
        if (sceneManager != null && mainLayoutController != null && currentManagerId != null) {
            OrderReviewController reviewCtrl = (OrderReviewController) sceneManager.loadFXMLIntoPane(
                mainLayoutController.getContentPane(), com.aims.core.shared.constants.FXMLPaths.PM_ORDER_REVIEW_SCREEN // Assuming FXMLPaths.PM_ORDER_REVIEW_SCREEN
            );
            reviewCtrl.setOrderToReview(order.getOrderId()); // Pass OrderID, controller will load details
            reviewCtrl.setCurrentManagerId(this.currentManagerId);
            reviewCtrl.setMainLayoutController(mainLayoutController);
            reviewCtrl.setOrderService(orderService); // Pass the service
            reviewCtrl.setSceneManager(sceneManager); // Pass the scene manager
            mainLayoutController.setHeaderTitle("Review Order - #" + order.getOrderId());
        }
    }

    @FXML
    void handlePrevPageAction(ActionEvent event) {
        if (currentPage > 1) {
            currentPage--;
            loadPendingOrders();
        }
    }

    @FXML
    void handleNextPageAction(ActionEvent event) {
        if (currentPage < totalPages) {
            currentPage++;
            loadPendingOrders();
        }
    }
}