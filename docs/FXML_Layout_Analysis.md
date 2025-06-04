# AIMS FXML Layout Structure Analysis

## Layout Hierarchy Mapping

### Level 1: Main Application Structure
```
main_layout.fxml (Root)
├── BorderPane (root container) ✓
│   ├── Top: VBox (MenuBar + Header)
│   ├── Center: BorderPane (dynamic content area) ✓
│   └── Bottom: Label (footer)
```

### Level 2: Main Content Screens

#### 2.1 Product/Catalog Screens
```
home_screen.fxml
├── BorderPane (root) ✓
│   ├── Top: VBox (title + search controls)
│   ├── Center: ScrollPane > FlowPane (products) ✓
│   └── Bottom: HBox (pagination)

product_search_results_screen.fxml
├── Similar structure to home_screen ✓
```

#### 2.2 Cart/Order Screens  
```
cart_screen.fxml
├── BorderPane (root) ✓
│   ├── Top: Label (title)
│   ├── Center: ScrollPane > VBox (cart items) ✓
│   └── Bottom: VBox (totals + actions)

order_summary_screen.fxml
├── BorderPane (root) ✓
│   ├── Top: Label (title)
│   ├── Center: ScrollPane > VBox (order details)
│   └── Bottom: HBox (action buttons)
```

#### 2.3 Admin/Management Screens
```
admin_product_list_screen.fxml
├── BorderPane (root) ✓
│   ├── Top: VBox (title + controls)
│   ├── Center: ScrollPane > VBox (product list)
│   └── Bottom: HBox (pagination)

pm_dashboard_screen.fxml
├── BorderPane (root) ✓
│   ├── Top: VBox (title + welcome)
│   └── Center: FlowPane (dashboard actions) ✓
```

#### 2.4 Form Screens
```
login_screen.fxml
├── VBox (root, CENTER alignment) ✓
│   ├── Label (title)
│   ├── GridPane (form fields) ✓
│   ├── Label (error message)
│   └── HBox (buttons, CENTER alignment) ✓

delivery_info_screen.fxml
├── BorderPane (root)
│   ├── Top: Label (title)  
│   ├── Center: ScrollPane > VBox (form)
│   └── Bottom: HBox (buttons)
```

### Level 3: Partial Components

#### 3.1 Product Components
```
product_card.fxml
├── VBox (root, CENTER alignment) ✓
│   ├── ImageView (product image)
│   ├── Label (title)
│   ├── Label (price)
│   ├── Label (availability)
│   └── Button (add to cart)
```

#### 3.2 Item Row Components
```
cart_item_row.fxml
├── HBox (root, CENTER_LEFT alignment) ✓
│   ├── ImageView (product image)
│   ├── VBox (product details)
│   ├── HBox (quantity controls)
│   └── Button (remove)

order_item_row.fxml  
├── Similar structure to cart_item_row ✓
```

#### 3.3 Dialog Components
```
confirmation_dialog.fxml
├── VBox (root, CENTER alignment) ✓
│   ├── ImageView (icon)
│   ├── Label (title)
│   ├── Label (message)
│   └── HBox (buttons, CENTER alignment) ✓

error_dialog.fxml
├── Similar structure to confirmation_dialog ✓
```

## Current Issues Analysis

### 🔴 Critical Issues
1. **Inconsistent Root Container Types**
   - Some forms use VBox when BorderPane would be better
   - Missing standard layout structure in some screens

2. **Responsive Behavior Gaps**  
   - Some containers lack proper fill behavior
   - Missing maxWidth/Height="Infinity" on key containers
   - ScrollPane fitToWidth/Height not consistently applied

3. **Alignment Inconsistencies**
   - Mixed alignment strategies across similar components
   - Some containers missing explicit alignment properties

### 🟡 Medium Issues
4. **Spacing and Padding Variations**
   - Inconsistent spacing values across screens
   - Missing standard padding patterns

5. **Sizing Constraint Gaps**
   - Some components lack proper min/max/pref sizing
   - Missing responsive sizing on form elements

### 🟢 Strengths
6. **Good Practices Found**
   - Main layout uses BorderPane effectively
   - Product grids use FlowPane for responsiveness
   - Dialog components are well-structured
   - ScrollPane usage is generally correct

## Priority Fix Categories

### High Priority (Structural)
- Standardize root container patterns
- Fix responsive fill behavior  
- Implement consistent alignment

### Medium Priority (Visual)
- Standardize spacing and padding
- Improve sizing constraints
- Enhance form layouts

### Low Priority (Polish)
- Fine-tune visual spacing
- Optimize component sizing
- Cross-screen consistency tweaks
