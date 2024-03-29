package com.picoral.gui.windows;

import com.picoral.Util;
import com.picoral.core.App;
import com.picoral.data.DataHandler;
import com.picoral.gui.popups.ChangeURL;
import com.picoral.gui.popups.ConfirmBox;
import com.picoral.models.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Controller for the view more about product window
 */
public class ViewProduct {

    /**
     * ViewProduct layout handler
     */
    private class ViewProductLayout extends AnchorPane {

        @FXML
        private TextField nameField;

        @FXML
        private TextField idField;

        @FXML
        private TextField priceField;

        @FXML
        private TextField categoryField;

        @FXML
        private TextField modelField;

        @FXML
        private TextField brandField;

        @FXML
        private TextField warrantyField;

        @FXML
        private TextField quantityField;

        @FXML
        private Button btnEdit;

        @FXML
        private Button btnSave;

        @FXML
        private Button btnCancel;

        @FXML
        private ImageView imageView;

        @FXML
        private Label noImgLabel;

        @FXML
        private VBox parent;

        @FXML
        private ProgressBar imgProgressbar;

        //Allow to revert to the old image if the user changes it to a invalid one
        private Image oldImage;

        //Load the .fxml
        public ViewProductLayout() {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/picoral/views/product.fxml"));
            loader.setRoot(this);
            loader.setController(this);

            try {
                loader.load();
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }

        }

        @FXML
        void initialize() {

            //Add listeners to the price and quantity fields, the only two which need data validation
            Util.Listeners.addPriceListener(priceField);
            Util.Listeners.addQuantityListener(quantityField);

            //Update fields values to the actual product info
            populateFields();
            addUniqueFields();

            //Set product image
            setImage();

            //Edit button on click
            btnEdit.setOnAction(e -> {

                changeEditMode();
                changeTextFieldDisabledState();

            });

            //Cancel edit button on click
            btnCancel.setOnAction(e -> {

                //Disable all the fields and populate the universal ones with the old values
                changeTextFieldDisabledState();
                populateFields();

                //Invert the current edit mode state
                changeEditMode();

                //Revert the unique fields data to theirs original state
                //Not the best performance wise since to revert the fields it's deleting and re-adding them
                //Using this to avoid having to make a switch statement for every category
                for (TextField uniqueField : uniqueFields) {
                    VBox parent = (VBox) uniqueField.getParent().getParent();
                    parent.getChildren().remove(uniqueField.getParent());
                }
                addUniqueFields();

            });

            //Save button on click
            btnSave.setOnAction(e -> {

                changeEditMode();
                changeTextFieldDisabledState();

                //Update the value
                product.setName(nameField.getText());
                product.setPrice(Double.parseDouble(priceField.getText()));
                product.setCategory(categoryField.getText());
                product.setModel(modelField.getText());
                product.setBrand(brandField.getText());
                product.setWarranty(warrantyField.getText());
                product.setQuantity(Integer.parseInt(quantityField.getText()));

                switch (product.getCategory()) {

                    case "Computer":

                        Computer computer = (Computer) product;

                        computer.setRam(uniqueFields.get(0).getText());
                        computer.setGpu(uniqueFields.get(1).getText());
                        computer.setCpu(uniqueFields.get(2).getText());
                        computer.setStorageType(uniqueFields.get(3).getText());

                        break;

                    case "TV":

                        TV tv = (TV) product;

                        tv.setSize(uniqueFields.get(0).getText());
                        tv.setResolution(uniqueFields.get(1).getText());

                        break;

                    case "Watch":

                        Watch watch = (Watch) product;

                        watch.setColor(uniqueFields.get(0).getText());
                        watch.setSize(uniqueFields.get(1).getText());

                        break;

                    case "Phone":

                        Phone phone = (Phone) product;

                        phone.setOs(uniqueFields.get(0).getText());
                        phone.setColor(uniqueFields.get(1).getText());

                        break;

                    default:
                        throw new RuntimeException("InvalidCategoryException");

                }

                //Save the new values and update the table
                dataHandler.updateProduct(product);

            });

            //Allow to close using esc
            this.setOnKeyReleased(event -> {

                if (event.getCode() == KeyCode.ESCAPE) {

                    stop();

                }

            });

            //Auto enter on edit mode
            if (editMode) {
                editMode = false; //Workaround to work properly since it's inverted once changeEditMode is called
                btnEdit.fire();
            }

        }

        /**
         * Shows the product's image and handles the progress bar/label
         */
        private void setImage() {
            setImage(false);
        }

        /**
         * Shows the product's image and handles the progress bar/label
         *
         * @param isUpdate If the image is being loaded for the first time or it is being updated
         */
        private void setImage(boolean isUpdate) {

            if (product.hasImage()) {

                imageView.setImage(product.getImage());

                //Right mouse btn menu
                ContextMenu cm = new ContextMenu() {{
                    getItems().add(new MenuItem("Change Image"));
                    getItems().add(new MenuItem("Copy URL"));
                    getItems().add(new MenuItem("Copy Image"));

                    //Edit Image
                    getItems().get(0).setOnAction(e -> {

                        oldImage = product.getImage();
                        new ChangeURL(viewWindow, product);
                        imageView.setImage(product.getImage());

                    });

                    //Copy url
                    getItems().get(1).setOnAction(e -> {

                        Clipboard.getSystemClipboard().setContent(new ClipboardContent() {{
                            putString(product.getImageURL());
                            putHtml(String.format("<a>%s</a>", product.getImageURL()));
                        }});

                    });

                    //Copy image
                    getItems().get(2).setOnAction(e -> {

                        Clipboard.getSystemClipboard().setContent(new ClipboardContent() {{
                            putImage(product.getImage());
                            putHtml(String.format("<img src=\"%s\">", product.getImageURL()));
                        }});

                    });

                }};

                if (product.isImageLoaded() && !isUpdate) {

                    //Avoid showing the label and progress bar if the image has already been loaded
                    noImgLabel.setVisible(false);
                    imgProgressbar.setVisible(false);

                } else {

                    //Make sure it starts at 0 since it may be an update
                    imgProgressbar.setProgress(0D);

                    //Image is still loading
                    imgProgressbar.setVisible(true);
                    noImgLabel.setText("The image is loading...");
                    noImgLabel.setVisible(true);

                }

                //Image loading progress listener
                product.getImage().progressProperty().addListener((observable, oldValue, progress) -> {

                    //Update the progress bar
                    imgProgressbar.setProgress(progress.doubleValue());

                    //Image finished loading
                    if (progress.doubleValue() == 1D) {

                        //Hide bar and label
                        imgProgressbar.setVisible(false);
                        noImgLabel.setVisible(false);

                        //Image is invalid - due to background loading can only be called after it was fully loaded
                        if (product.getImage().isError()) {

                            //Show error
                            noImgLabel.setText("The URL you entered is invalid\nThe old image will be used");
                            noImgLabel.setVisible(true);

                            //JavaFX TimeLine to reset the image after a delay
                            Timeline timeline = new Timeline(new KeyFrame(Duration.ONE, event -> {

                                //Reset the image view and product image attribute
                                imageView.setImage(oldImage);
                                product.setImage(oldImage);

                                //Hide the error label and save the changes to the JSON
                                noImgLabel.setVisible(false);
                                dataHandler.updateProduct(product);

                            }));

                            timeline.setCycleCount(1);
                            timeline.setDelay(Duration.seconds(2D)); //Delay of 2 seconds
                            timeline.play();

                        }

                    }

                });

                //Right mouse btn on image handling
                imageView.setOnContextMenuRequested(event -> {

                    if (!product.isImageLoaded()) {
                        event.consume();
                        return;
                    }

                    if (cm.isShowing()) {
                        cm.hide();
                    }

                    cm.show(imageView, event.getScreenX(), event.getScreenY());

                });

            }
        }

        /**
         * Populate the non-unique text fields with the product's info
         */
        private void populateFields() {

            nameField.setText(product.getName());
            idField.setText(product.getID());
            priceField.setText(Double.toString(product.getPrice()));
            categoryField.setText(product.getCategory());
            modelField.setText(product.getModel());
            brandField.setText(product.getBrand());
            warrantyField.setText(product.getWarranty());
            quantityField.setText(Integer.toString(product.getQuantity()));

        }

        /**
         * Adds the unique fields based on the product's category
         */
        private void addUniqueFields() {

            switch (product.getCategory()) {

                case "Computer":

                    Computer c = (Computer) product;

                    for (String property : Computer.getPropertiesArr()) {

                        HBox hb = new HBox();
                        hb.setAlignment(Pos.CENTER);
                        hb.setSpacing(30);
                        hb.setPrefWidth(200);
                        hb.setPrefHeight(100);

                        Label lb = new Label(property);

                        Region region = new Region();
                        HBox.setHgrow(region, Priority.ALWAYS);

                        TextField tf = new TextField();
                        tf.setText(c.getPropertyByName(property));
                        tf.setAlignment(Pos.CENTER);
                        tf.setDisable(true);

                        hb.getChildren().addAll(lb, region, tf);

                        VBox parent = (VBox) quantityField.getParent().getParent();

                        uniqueFields.add(tf);

                        parent.getChildren().add(
                                parent.getChildren().size() - 1,
                                hb
                        );

                    }

                    break;

                case "TV":

                    TV tv = (TV) product;

                    for (String property : TV.getPropertiesArr()) {

                        HBox hb = new HBox();
                        hb.setAlignment(Pos.CENTER);
                        hb.setSpacing(30);
                        hb.setPrefWidth(200);
                        hb.setPrefHeight(100);

                        Label lb = new Label(property);

                        Region region = new Region();
                        HBox.setHgrow(region, Priority.ALWAYS);

                        TextField tf = new TextField();
                        tf.setText(tv.getPropertyByName(property));
                        tf.setAlignment(Pos.CENTER);
                        tf.setDisable(true);

                        hb.getChildren().addAll(lb, region, tf);

                        VBox parent = (VBox) quantityField.getParent().getParent();

                        uniqueFields.add(tf);

                        parent.getChildren().add(
                                parent.getChildren().size() - 1,
                                hb
                        );

                    }

                    break;

                case "Watch":

                    Watch w = (Watch) product;

                    for (String property : Watch.getPropertiesArr()) {

                        HBox hb = new HBox();
                        hb.setAlignment(Pos.CENTER);
                        hb.setSpacing(30);
                        hb.setPrefWidth(200);
                        hb.setPrefHeight(100);

                        Label lb = new Label(property);

                        Region region = new Region();
                        HBox.setHgrow(region, Priority.ALWAYS);

                        TextField tf = new TextField();
                        tf.setText(w.getPropertyByName(property));
                        tf.setAlignment(Pos.CENTER);
                        tf.setDisable(true);

                        hb.getChildren().addAll(lb, region, tf);

                        VBox parent = (VBox) quantityField.getParent().getParent();

                        uniqueFields.add(tf);

                        parent.getChildren().add(
                                parent.getChildren().size() - 1,
                                hb
                        );

                    }

                    break;

                case "Phone":

                    Phone p = (Phone) product;

                    for (String property : Phone.getPropertiesArr()) {

                        HBox hb = new HBox();
                        hb.setAlignment(Pos.CENTER);
                        hb.setSpacing(30);
                        hb.setPrefWidth(200);
                        hb.setPrefHeight(100);

                        Label lb = new Label(property);

                        Region region = new Region();
                        HBox.setHgrow(region, Priority.ALWAYS);

                        TextField tf = new TextField();
                        tf.setText(p.getPropertyByName(property));
                        tf.setAlignment(Pos.CENTER);
                        tf.setDisable(true);

                        hb.getChildren().addAll(lb, region, tf);

                        VBox parent = (VBox) quantityField.getParent().getParent();

                        uniqueFields.add(tf);

                        parent.getChildren().add(
                                parent.getChildren().size() - 1,
                                hb
                        );

                    }

                    break;

                default:
                    throw new RuntimeException("InvalidCategoryException");
            }

        }

        /**
         * Inverts the text fields disabled state
         */
        private void changeTextFieldDisabledState() {
            for (Node hb : parent.getChildren()) {

                if (hb instanceof HBox) {

                    for (Node tf : ((HBox) hb).getChildren()) {

                        if (tf instanceof TextField) {

                            boolean currentState = tf.isDisabled();

                            try {
                                if (!tf.getId().equals("idField")) {
                                    tf.setDisable(!currentState);
                                }
                            } catch (Exception ignored) {
                                tf.setDisable(!currentState);
                            }

                        }

                    }

                }

            }
        }

        /**
         * Inverts the edit mode and sets the buttons' visibility state accordingly
         */
        private void changeEditMode() {
            editMode = !editMode;

            btnEdit.setVisible(!editMode);
            btnSave.setVisible(editMode);
            btnCancel.setVisible(editMode);
        }

    }

    private final List<TextField> uniqueFields = new LinkedList<>();
    private final Stage window;
    private final DataHandler dataHandler = App.dataHandler;
    private final Product product;
    private boolean editMode;

    //Needed in order to re-show the progress bar & label if the user changes the image
    private final ViewProduct viewWindow = this;
    private final ViewProductLayout layout;

    /**
     * Constructor for the ViewProduct window. Once called will create a new window displaying the
     * specified product and its properties
     *
     * @param product     Product to be displayed
     * @param editMode    If true, the window will open as if the edit button was clicked (on edit mode)
     */
    public ViewProduct(Product product, boolean editMode) {

        if (!product.hasImage()) {
            product.loadImage();
        }

        this.product = product;

        this.editMode = editMode;


        //Stage and layout initialization
        ViewProductLayout vp = new ViewProductLayout();
        layout = vp;
        window = new Stage();

        //Avoid skipping stop handling if closed through the X
        window.setOnCloseRequest(e -> {
            e.consume();
            stop();
        });

        //Window properties
        window.setResizable(false);
        window.setTitle(product.getName());
        window.setScene(new Scene(vp));

        //Show window
        window.show();

    }

    /**
     * Constructor without explicit declaring the edit mode to false
     *
     * @param product     Product to display
     */
    public ViewProduct(Product product) {
        this(product, false);
    }

    private void stop() {

        if (!this.editMode) {
            window.close();
        } else if (ConfirmBox.getConfirmation("Do you really want to leave? Any unsaved changes will be lost.")) {
            window.close();
        }

    }

    /**
     * Public function to force image update. Through this it's possible to re-use the code
     * to show the progress bar/label if the image is changed by the user
     */
    public void updateImage() {
        layout.setImage(true);
    }

}
