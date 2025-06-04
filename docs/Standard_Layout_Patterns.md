# AIMS Standard Layout Patterns

## Container Hierarchy Standards

### 1. Full-Screen Content Pattern (Main Screens)
```xml
<BorderPane fx:id="rootPane" maxHeight="Infinity" maxWidth="Infinity" 
           minHeight="500.0" minWidth="800.0" prefHeight="600.0" prefWidth="1000.0">
    <padding>
        <Insets bottom="10.0" left="10.0" right="10.0" top="10.0" />
    </padding>
    <top>
        <!-- Header content with title and controls -->
        <VBox spacing="10.0" alignment="TOP_CENTER">
            <Label fx:id="screenTitle" alignment="CENTER" maxWidth="Infinity"/>
            <!-- Additional header controls if needed -->
        </VBox>
    </top>
    <center>
        <!-- Main scrollable content area -->
        <ScrollPane fx:id="mainScrollPane" fitToWidth="true" fitToHeight="true" 
                   hbarPolicy="NEVER" vbarPolicy="AS_NEEDED" 
                   maxHeight="Infinity" maxWidth="Infinity">
            <!-- Content container (VBox for lists, FlowPane for grids) -->
        </ScrollPane>
    </center>
    <bottom>
        <!-- Footer actions/controls -->
        <HBox spacing="10.0" alignment="CENTER" 
              minHeight="60.0" maxWidth="Infinity">
            <!-- Action buttons or pagination -->
        </HBox>
    </bottom>
</BorderPane>
```

### 2. Form Screen Pattern (Login, Settings, etc.)
```xml
<VBox fx:id="formPane" alignment="CENTER" spacing="20.0" 
      maxWidth="500.0" minWidth="400.0" prefWidth="450.0">
    <padding>
        <Insets bottom="30.0" left="50.0" right="50.0" top="30.0" />
    </padding>
    
    <!-- Form title -->
    <Label fx:id="formTitle" alignment="CENTER" maxWidth="Infinity">
        <VBox.margin>
            <Insets bottom="20.0" />
        </VBox.margin>
    </Label>
    
    <!-- Form content area -->
    <VBox spacing="15.0" alignment="CENTER" maxWidth="400.0">
        <!-- Form controls go here -->
    </VBox>
    
    <!-- Error/status message area -->
    <Label fx:id="messageLabel" wrapText="true" textAlignment="CENTER" 
           managed="false" visible="false" maxWidth="350.0"/>
    
    <!-- Action buttons -->
    <HBox alignment="CENTER" spacing="15.0">
        <!-- Buttons with consistent sizing -->
        <Button fx:id="primaryButton" minWidth="100.0"/>
        <Button fx:id="secondaryButton" minWidth="100.0"/>
    </HBox>
</VBox>
```

### 3. Dialog Pattern (Confirmations, Alerts)
```xml
<VBox fx:id="dialogPane" alignment="CENTER" spacing="20.0" 
      prefWidth="400.0" maxWidth="500.0">
    <padding>
        <Insets bottom="20.0" left="20.0" right="20.0" top="20.0" />
    </padding>
    
    <!-- Icon (optional) -->
    <ImageView fx:id="iconImageView" fitHeight="48.0" fitWidth="48.0" 
              pickOnBounds="true" preserveRatio="true"/>
    
    <!-- Title -->
    <Label fx:id="titleLabel" alignment="CENTER" maxWidth="Infinity"/>
    
    <!-- Message content -->
    <Label fx:id="messageLabel" wrapText="true" textAlignment="CENTER" 
           maxWidth="350.0"/>
    
    <!-- Action buttons -->
    <HBox alignment="CENTER" spacing="15.0">
        <Button fx:id="confirmButton" minWidth="80.0"/>
        <Button fx:id="cancelButton" minWidth="80.0"/>
    </HBox>
</VBox>
```

### 4. List Item Pattern (Cart items, Order items)
```xml
<HBox fx:id="itemRow" alignment="CENTER_LEFT" spacing="15.0" 
      minHeight="80.0" maxWidth="Infinity">
    <padding>
        <Insets bottom="10.0" left="10.0" right="10.0" top="10.0" />
    </padding>
    
    <!-- Item image -->
    <ImageView fx:id="itemImage" fitHeight="60.0" fitWidth="60.0" 
              pickOnBounds="true" preserveRatio="true"/>
    
    <!-- Item details (grows to fill space) -->
    <VBox spacing="5.0" HBox.hgrow="ALWAYS" alignment="CENTER_LEFT">
        <Label fx:id="itemTitle" maxWidth="Infinity"/>
        <Label fx:id="itemDetails"/>
    </VBox>
    
    <!-- Quantity/controls area -->
    <HBox spacing="10.0" alignment="CENTER">
        <!-- Quantity controls or other actions -->
    </HBox>
    
    <!-- Action button -->
    <Button fx:id="actionButton" minWidth="80.0"/>
</HBox>
```

### 5. Product Card Pattern (Grid displays)
```xml
<VBox fx:id="productCard" alignment="CENTER" spacing="10.0"
      minHeight="320.0" maxHeight="350.0" 
      minWidth="250.0" maxWidth="280.0" 
      prefWidth="260.0" prefHeight="330.0">
    <padding>
        <Insets bottom="10.0" left="10.0" right="10.0" top="10.0"/>
    </padding>
    
    <!-- Product image -->
    <ImageView fx:id="productImage" fitHeight="160.0" fitWidth="120.0" 
              pickOnBounds="true" preserveRatio="true">
        <VBox.margin>
            <Insets bottom="10.0"/>
        </VBox.margin>
    </ImageView>
    
    <!-- Product details -->
    <Label fx:id="productTitle" text="Product Title" wrapText="true" 
           textAlignment="CENTER" minHeight="40.0" maxWidth="220.0"/>
    <Label fx:id="productPrice" alignment="CENTER"/>
    <Label fx:id="productStatus" alignment="CENTER"/>
    
    <!-- Action button -->
    <Button fx:id="actionButton" maxWidth="Infinity" minWidth="200.0" 
            VBox.vgrow="NEVER"/>
</VBox>
```

## Alignment Standards

### Container Alignment Rules
- **Root containers**: No specific alignment (let content determine)
- **Form containers**: CENTER alignment for forms, CENTER_LEFT for content
- **Button containers**: CENTER alignment
- **Content lists**: TOP_LEFT or CENTER_LEFT
- **Header/footer**: CENTER alignment

### Spacing Standards
- **Major sections**: 20px spacing
- **Form elements**: 15px spacing  
- **Button groups**: 15px spacing
- **List items**: 10px spacing
- **Inline elements**: 5-10px spacing

### Padding Standards
- **Screen containers**: 10px all sides
- **Form containers**: 30-50px all sides
- **Dialog containers**: 20px all sides
- **Component containers**: 10px all sides
- **Compact components**: 5px all sides

### Sizing Standards
- **Screen dimensions**: 
  - Min: 800x500, Pref: 1000x600
  - Max: Infinity for main containers
- **Form dimensions**:
  - Max width: 500px
  - Min width: 400px
- **Dialog dimensions**:
  - Pref width: 400px
  - Max width: 500px
- **Button dimensions**:
  - Min width: 80-100px for action buttons
  - Max width: 200px for card buttons

## Responsive Behavior Rules

### Fill Behavior
- Root containers: maxWidth/Height="Infinity"
- Content areas: HBox.hgrow="ALWAYS" or VBox.vgrow="ALWAYS"
- ScrollPanes: fitToWidth="true", fitToHeight="true"

### Constraint Priorities
1. **Must fill available space**: Main content areas
2. **Fixed size**: Buttons, images, specific UI elements  
3. **Flexible within limits**: Form fields, text areas
4. **Responsive to content**: Labels, dynamic content
