package org.nlogo.modelingcommons;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.nlogo.awt.UserCancelException;
import org.nlogo.swing.FileDialog;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class UploadDialog extends JDialog {
  private JPanel contentPane;
  private JButton uploadModelButton;
  private JButton cancelButton;
  private JButton logoutButton;
  private JTextField modelNameField;
  private JLabel errorLabel;
  private JLabel personNameLabel;
  private JComboBox groupComboBox;
  private DisableableComboBox visibilityComboBox;
  private DisableableComboBox changeabilityComboBox;
  private JRadioButton useCurrentViewRadioButton;
  private JRadioButton autoGenerateViewRadioButton;
  private JRadioButton noPreviewRadioButton;
  private JRadioButton imageFromFileRadioButton;
  private JButton selectFileButton;
  private JLabel selectedFileLabel;
  private JLabel autoGenerateDisabledExplanation;
  private JRadioButton newModelRadioButton;
  private JRadioButton childOfExisitingModelRadioButton;
  private JRadioButton newVersionOfExisitingRadioButton;
  private DisableableComboBox exisitingModelNameComboBox;
  private JComboBox comboBox2;
  private JTextField exisitingModelNameSearchField;
  private JTextField descriptionTextField;
  private JLabel modelNameLabel;
  private JLabel exisitingModelNameLabel;
  private JLabel descriptionLabel;
  private JLabel modelGroupLabel;
  private JLabel visibilityLabel;
  private JLabel changeabilityLabel;
  private JLabel previewImageLabel;
  private ButtonGroup uploadTypeButtonGroup;
  private ButtonGroup previewImageButtonGroup;
  private ModelingCommons communicator;
  private Frame frame;
  private int groupPermissionIndex;
  private int userPermissionIndex;
  private int everyonePermissionIndex;
  private String uploadImageFilePath;
  private Request searchRequest;

  UploadDialog(final Frame frame, final ModelingCommons communicator, String errorLabelText, boolean enableAutoGeneratePreviewImage) {
    super(frame, "Upload Model to Modeling Commons", true);

    this.communicator = communicator;
    this.frame = frame;
    errorLabel.setText(errorLabelText);
    personNameLabel.setText("Hello " + communicator.getPerson().getFirstName() + " " + communicator.getPerson().getLastName());


    setContentPane(contentPane);
    setModal(true);
    getRootPane().setDefaultButton(uploadModelButton);

    uploadModelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        onOK();
      }
    });

    cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        onCancel();
      }
    });

    logoutButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        dispose();
        ModelingCommons.LogoutRequest request = communicator.new LogoutRequest() {
          @Override
          protected void onLogout(String status) {
            communicator.promptForLogin();
          }
        };
        request.execute();
      }
    });

    List<Group> groups = new ArrayList<Group>(communicator.getGroups());
    groups.add(0, null);
    groupComboBox.setModel(new DefaultComboBoxModel(groups.toArray()));
    everyonePermissionIndex = visibilityComboBox.addItem(Permission.getPermissions().get("a"), true);
    changeabilityComboBox.addItem(Permission.getPermissions().get("a"), true);
    groupPermissionIndex = visibilityComboBox.addItem(Permission.getPermissions().get("g"), false);
    changeabilityComboBox.addItem(Permission.getPermissions().get("g"), false);
    userPermissionIndex = visibilityComboBox.addItem(Permission.getPermissions().get("u"), true);
    changeabilityComboBox.addItem(Permission.getPermissions().get("u"), true);


    groupComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        boolean groupSelected = !(groupComboBox.getSelectedItem() == null);
        visibilityComboBox.setIndexEnabled(groupPermissionIndex, groupSelected);
        changeabilityComboBox.setIndexEnabled(groupPermissionIndex, groupSelected);

        Permission visibility = (Permission) (visibilityComboBox.getSelectedObject());
        if (!groupSelected && visibility.getId().equals("g")) {
          visibilityComboBox.setSelectedIndex(userPermissionIndex);
        }

        Permission changeability = (Permission) (changeabilityComboBox.getSelectedObject());
        if (!groupSelected && changeability.getId().equals("g")) {
          changeabilityComboBox.setSelectedIndex(userPermissionIndex);
        }


      }
    });

    selectFileButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        try {
          uploadImageFilePath = FileDialog.show(frame, "Select image to use as preview image", java.awt.FileDialog.LOAD);
          String toSet = uploadImageFilePath;
          FontMetrics metrics = selectedFileLabel.getFontMetrics(selectedFileLabel.getFont());
          while (metrics.stringWidth(toSet) > selectedFileLabel.getMaximumSize().width) {
            toSet = "\u2026" + toSet.substring(2);
          }
          selectedFileLabel.setText(toSet);
        } catch (UserCancelException e) {
        }
      }
    });
    useCurrentViewRadioButton.setSelected(true);
    fileUploadSetEnabled(false);
    imageFromFileRadioButton.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        fileUploadSetEnabled(imageFromFileRadioButton.isSelected());
      }
    });
    if (!enableAutoGeneratePreviewImage) {
      autoGenerateViewRadioButton.setEnabled(false);
    } else {
      autoGenerateDisabledExplanation.setVisible(false);
    }


    exisitingModelNameSearchField.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent documentEvent) {
        updateExistingModelNameComboBox(exisitingModelNameSearchField.getText());
      }

      @Override
      public void removeUpdate(DocumentEvent documentEvent) {
        updateExistingModelNameComboBox(exisitingModelNameSearchField.getText());
      }

      @Override
      public void changedUpdate(DocumentEvent documentEvent) {

      }
    });

    newModelRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        if (newModelRadioButton.isSelected()) {
          setNewModelMode();
        }
      }
    });
    newVersionOfExisitingRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        if (newVersionOfExisitingRadioButton.isSelected()) {
          setNewVersionMode();
        }
      }
    });


    childOfExisitingModelRadioButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        if (childOfExisitingModelRadioButton.isSelected()) {
          setChildMode();
        }
      }
    });
    newModelRadioButton.setSelected(true);
    setNewModelMode();
    updateExistingModelNameComboBox("");

    // call onCancel() when cross is clicked
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        onCancel();
      }
    });

    // call onCancel() on ESCAPE
    contentPane.registerKeyboardAction(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        onCancel();
      }
    }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

    this.pack();
    this.setLocationRelativeTo(frame);
    this.setResizable(false);
  }

  private void clearExistingModelNameComboBox() {
    exisitingModelNameComboBox.removeAllItems();
    exisitingModelNameComboBox.hidePopup();
  }

  private void updateExistingModelNameComboBox(final String queryString) {
    if (searchRequest != null) {
      searchRequest.abort();
      searchRequest = null;
    }
    exisitingModelNameComboBox.removeAllItems();
    exisitingModelNameComboBox.hidePopup();

    if (queryString.length() > 0) {
      exisitingModelNameComboBox.addItem("Searching", false);
    } else {
      exisitingModelNameComboBox.addItem("Enter name of existing model", false);
      return;
    }

    boolean ensureChangeabilityPermission = getSelectedUploadType() == NewModelType.NEW_VERSION;
    searchRequest = communicator.new SearchForModelsRequest(queryString, 10, ensureChangeabilityPermission) {
      @Override
      protected void onSearchResults(List<Model> models) {
        searchRequest = null;
        exisitingModelNameComboBox.removeAllItems();
        if (models.size() > 0) {

          for (Model model : models) {
            exisitingModelNameComboBox.addItem(model, true);


          }
          boolean currentFocus = exisitingModelNameSearchField.hasFocus();
          exisitingModelNameComboBox.showPopup();
          if (currentFocus) {
            exisitingModelNameSearchField.requestFocus();
          }

        } else {
          exisitingModelNameComboBox.addItem("No existing models found", false);
        }
      }
    };
    searchRequest.execute();
  }

  private boolean isValidInput() {
    NewModelType uploadType = getSelectedUploadType();
    //Check name
    if (uploadType == NewModelType.NEW || uploadType == NewModelType.CHILD) {
      if (modelNameField.getText().trim().length() == 0) {
        errorLabel.setText("Missing model name");
        return false;
      }
    }

    //Check existing model is valid
    if (uploadType == NewModelType.NEW_VERSION || uploadType == NewModelType.CHILD) {
      if (getSelectedExistingModel() == null) {
        if (uploadType == NewModelType.NEW_VERSION) {
          errorLabel.setText("Must select an existing model to make a new version of");
        } else {
          errorLabel.setText("Must select an existing parent model");
        }
        return false;
      }
    }

    //Check description
    if (uploadType == NewModelType.NEW_VERSION || uploadType == NewModelType.CHILD) {
      if (descriptionTextField.getText().trim().length() == 0) {
        errorLabel.setText("Description cannot be blank");
        return false;
      }
    }


    return true;
  }

  private void onOK() {
    if (!isValidInput()) {
      return;
    }
    dispose();

    String modelName = modelNameField.getText().trim();
    String description = descriptionTextField.getText().trim();
    Group group = (Group) groupComboBox.getSelectedItem();
    Permission visibility = (Permission) visibilityComboBox.getSelectedObject();
    Permission changeability = (Permission) changeabilityComboBox.getSelectedObject();
    NewModelType uploadType = getSelectedUploadType();
    Model selectedExistingModel = getSelectedExistingModel();
    int existingModelId = -1;
    if (selectedExistingModel != null) {
      existingModelId = selectedExistingModel.getId();
    }
    Image previewImage = null;
    if (useCurrentViewRadioButton.isSelected()) {
      previewImage = communicator.getCurrentModelViewImage();
    } else if (imageFromFileRadioButton.isSelected()) {
      if (uploadImageFilePath != null) {
        previewImage = new FileImage(uploadImageFilePath);
      }
    } else if (autoGenerateViewRadioButton.isSelected()) {
      previewImage = communicator.getAutoGeneratedModelImage();
    }

    if (uploadType == NewModelType.NEW) {
      ModelingCommons.UploadModelRequest request = communicator.new UploadModelRequest(
          modelName,
          group,
          visibility,
          changeability,
          previewImage
      ) {
        @Override
        protected void onUploaded(String status) {
          if (status.equals("NOT_LOGGED_IN")) {
            communicator.promptForLogin();
          } else if (status.equals("MISSING_PARAMETERS")) {
            communicator.promptForUpload("Missing model name");
          } else if (status.equals("MODEL_NOT_SAVED")) {
            communicator.promptForUpload("Server error");
          } else if (status.equals("CONNECTION_ERROR")) {
            communicator.promptForUpload("Error connecting to Modeling Commons");
          } else if (status.equals("SUCCESS")) {
            communicator.promptForSuccess();
          } else if (status.equals("INVALID_PREVIEW_IMAGE")) {
            communicator.promptForUpload("Invalid preview image");
          } else if (status.equals("SUCCESS_PREVIEW_NOT_SAVED")) {
            communicator.promptForSuccess("The model was uploaded, but the preview image was not saved");
          } else {
            communicator.promptForUpload("Unknown server error");
          }
        }
      };
      request.execute();
    } else if (uploadType == NewModelType.NEW_VERSION || uploadType == NewModelType.CHILD) {
      ModelingCommons.UpdateModelRequest request = communicator.new UpdateModelRequest(existingModelId, modelName, description, uploadType) {
        @Override
        protected void onUploaded(String status) {
          if (status.equals("NOT_LOGGED_IN")) {
            communicator.promptForLogin();
          } else if (status.equals("MISSING_PARAMETERS")) {
            communicator.promptForUpload("Missing parameters");
          } else if (status.equals("MODEL_NOT_SAVED")) {
            communicator.promptForUpload("Server error");
          } else if (status.equals("CONNECTION_ERROR")) {
            communicator.promptForUpload("Error connecting to Modeling Commons");
          } else if (status.equals("SUCCESS")) {
            communicator.promptForSuccess();
          } else {
            communicator.promptForUpload("Unknown server error");
          }
        }
      };
      request.execute();
    }

  }

  private void onCancel() {
    dispose();
  }


  private NewModelType getSelectedUploadType() {
    if (newModelRadioButton.isSelected()) {
      return NewModelType.NEW;
    } else if (newVersionOfExisitingRadioButton.isSelected()) {
      return NewModelType.NEW_VERSION;
    } else if (childOfExisitingModelRadioButton.isSelected()) {
      return NewModelType.CHILD;
    } else {
      return null;
    }
  }

  private Model getSelectedExistingModel() {
    Object selectedModel = exisitingModelNameComboBox.getSelectedObject();
    if (selectedModel != null && selectedModel instanceof Model) {
      return (Model) selectedModel;
    } else {
      return null;
    }
  }

  private void modelNameSetEnabled(boolean enabled) {
    modelNameField.setEnabled(enabled);
    modelNameLabel.setEnabled(enabled);
  }

  private void existingModelSetEnabled(boolean enabled) {
    exisitingModelNameSearchField.setEnabled(enabled);
    exisitingModelNameComboBox.setEnabled(enabled);
    exisitingModelNameLabel.setEnabled(enabled);
    exisitingModelNameSearchField.setText("");
  }

  private void descriptionSetEnabled(boolean enabled) {
    descriptionTextField.setEnabled(enabled);
    descriptionLabel.setEnabled(enabled);
  }

  private void permissionsSetEnabled(boolean enabled) {
    groupComboBox.setEnabled(enabled);
    visibilityComboBox.setEnabled(enabled);
    changeabilityComboBox.setEnabled(enabled);
    modelGroupLabel.setEnabled(enabled);
    visibilityLabel.setEnabled(enabled);
    changeabilityLabel.setEnabled(enabled);
  }

  private void previewImageSetEnabled(boolean enabled) {
    useCurrentViewRadioButton.setEnabled(enabled);
    autoGenerateViewRadioButton.setEnabled(enabled);
    imageFromFileRadioButton.setEnabled(enabled);
    fileUploadSetEnabled(enabled && imageFromFileRadioButton.isSelected());
    noPreviewRadioButton.setEnabled(enabled);
    previewImageLabel.setEnabled(enabled);
    autoGenerateDisabledExplanation.setEnabled(enabled);

  }

  private void fileUploadSetEnabled(boolean enabled) {
    selectedFileLabel.setEnabled(enabled);
    selectFileButton.setEnabled(enabled);
  }

  private void setNewModelMode() {
    modelNameSetEnabled(true);
    existingModelSetEnabled(false);
    descriptionSetEnabled(false);
    permissionsSetEnabled(true);
    previewImageSetEnabled(true);
  }

  private void setNewVersionMode() {
    modelNameSetEnabled(false);
    existingModelSetEnabled(true);
    descriptionSetEnabled(true);
    permissionsSetEnabled(false);
    previewImageSetEnabled(false);
  }

  private void setChildMode() {
    modelNameSetEnabled(true);
    existingModelSetEnabled(true);
    descriptionSetEnabled(true);
    permissionsSetEnabled(false);
    previewImageSetEnabled(false);
  }

  {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
    $$$setupUI$$$();
  }

  /**
   * Method generated by IntelliJ IDEA GUI Designer
   * >>> IMPORTANT!! <<<
   * DO NOT edit this method OR call it in your code!
   *
   * @noinspection ALL
   */
  private void $$$setupUI$$$() {
    contentPane = new JPanel();
    contentPane.setLayout(new GridLayoutManager(3, 3, new Insets(10, 10, 10, 10), -1, -1));
    contentPane.setRequestFocusEnabled(false);
    final JPanel panel1 = new JPanel();
    panel1.setLayout(new GridLayoutManager(18, 2, new Insets(0, 0, 0, 0), -1, -1));
    contentPane.add(panel1, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    personNameLabel = new JLabel();
    personNameLabel.setText("Hello Firstname Lastname");
    panel1.add(personNameLabel, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label1 = new JLabel();
    label1.setText("Upload As");
    panel1.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    modelGroupLabel = new JLabel();
    modelGroupLabel.setText("Model Group");
    panel1.add(modelGroupLabel, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    visibilityLabel = new JLabel();
    visibilityLabel.setText("Visible By");
    panel1.add(visibilityLabel, new GridConstraints(10, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    changeabilityLabel = new JLabel();
    changeabilityLabel.setText("Changeable By");
    panel1.add(changeabilityLabel, new GridConstraints(11, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final Spacer spacer1 = new Spacer();
    panel1.add(spacer1, new GridConstraints(8, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(-1, 60), null, 0, false));
    final Spacer spacer2 = new Spacer();
    panel1.add(spacer2, new GridConstraints(12, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    previewImageLabel = new JLabel();
    previewImageLabel.setText("Preview Image");
    panel1.add(previewImageLabel, new GridConstraints(13, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    errorLabel = new JLabel();
    errorLabel.setForeground(new Color(-65536));
    errorLabel.setText("Error Message");
    panel1.add(errorLabel, new GridConstraints(17, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    newModelRadioButton = new JRadioButton();
    newModelRadioButton.setText("New model");
    panel1.add(newModelRadioButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    childOfExisitingModelRadioButton = new JRadioButton();
    childOfExisitingModelRadioButton.setText("Child of exisiting model");
    panel1.add(childOfExisitingModelRadioButton, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    newVersionOfExisitingRadioButton = new JRadioButton();
    newVersionOfExisitingRadioButton.setText("New version of exisiting model");
    panel1.add(newVersionOfExisitingRadioButton, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    groupComboBox = new JComboBox();
    panel1.add(groupComboBox, new GridConstraints(9, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    visibilityComboBox = new DisableableComboBox();
    panel1.add(visibilityComboBox, new GridConstraints(10, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    changeabilityComboBox = new DisableableComboBox();
    panel1.add(changeabilityComboBox, new GridConstraints(11, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    useCurrentViewRadioButton = new JRadioButton();
    useCurrentViewRadioButton.setActionCommand("USE_CURRENT_VIEW");
    useCurrentViewRadioButton.setEnabled(true);
    useCurrentViewRadioButton.setSelected(false);
    useCurrentViewRadioButton.setText("Use current view");
    panel1.add(useCurrentViewRadioButton, new GridConstraints(13, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JPanel panel2 = new JPanel();
    panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
    panel1.add(panel2, new GridConstraints(14, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    autoGenerateViewRadioButton = new JRadioButton();
    autoGenerateViewRadioButton.setActionCommand("AUTO_GENERATE_VIEW");
    autoGenerateViewRadioButton.setText("Auto-generate view");
    panel2.add(autoGenerateViewRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    autoGenerateDisabledExplanation = new JLabel();
    autoGenerateDisabledExplanation.setText("Setup and go procedures must be defined to auto-generate");
    panel2.add(autoGenerateDisabledExplanation, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JPanel panel3 = new JPanel();
    panel3.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), 10, -1));
    panel1.add(panel3, new GridConstraints(15, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    imageFromFileRadioButton = new JRadioButton();
    imageFromFileRadioButton.setActionCommand("IMAGE_FROM_FILE");
    imageFromFileRadioButton.setText("Image from file");
    panel3.add(imageFromFileRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    selectedFileLabel = new JLabel();
    selectedFileLabel.setHorizontalTextPosition(10);
    selectedFileLabel.setText("No file selected");
    panel3.add(selectedFileLabel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, new Dimension(300, -1), 0, false));
    selectFileButton = new JButton();
    selectFileButton.setMargin(new Insets(0, 2, 0, 2));
    selectFileButton.setText("Select File");
    panel3.add(selectFileButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    noPreviewRadioButton = new JRadioButton();
    noPreviewRadioButton.setActionCommand("NO_PREVIEW");
    noPreviewRadioButton.setText("No preview");
    panel1.add(noPreviewRadioButton, new GridConstraints(16, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    modelNameLabel = new JLabel();
    modelNameLabel.setText("New Model Name");
    panel1.add(modelNameLabel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    exisitingModelNameLabel = new JLabel();
    exisitingModelNameLabel.setText("Exisiting Model Name");
    panel1.add(exisitingModelNameLabel, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    modelNameField = new JTextField();
    panel1.add(modelNameField, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
    exisitingModelNameSearchField = new JTextField();
    panel1.add(exisitingModelNameSearchField, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
    exisitingModelNameComboBox = new DisableableComboBox();
    panel1.add(exisitingModelNameComboBox, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    descriptionTextField = new JTextField();
    panel1.add(descriptionTextField, new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
    descriptionLabel = new JLabel();
    descriptionLabel.setText("Short Comment");
    panel1.add(descriptionLabel, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JPanel panel4 = new JPanel();
    panel4.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
    contentPane.add(panel4, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_SOUTHEAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    uploadModelButton = new JButton();
    uploadModelButton.setText("Upload Model");
    panel4.add(uploadModelButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    cancelButton = new JButton();
    cancelButton.setText("Cancel");
    panel4.add(cancelButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final Spacer spacer3 = new Spacer();
    contentPane.add(spacer3, new GridConstraints(1, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    logoutButton = new JButton();
    logoutButton.setText("Logout");
    contentPane.add(logoutButton, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    previewImageButtonGroup = new ButtonGroup();
    previewImageButtonGroup.add(imageFromFileRadioButton);
    previewImageButtonGroup.add(autoGenerateViewRadioButton);
    previewImageButtonGroup.add(useCurrentViewRadioButton);
    previewImageButtonGroup.add(noPreviewRadioButton);
    uploadTypeButtonGroup = new ButtonGroup();
    uploadTypeButtonGroup.add(newModelRadioButton);
    uploadTypeButtonGroup.add(childOfExisitingModelRadioButton);
    uploadTypeButtonGroup.add(newVersionOfExisitingRadioButton);
  }

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return contentPane;
  }
}
