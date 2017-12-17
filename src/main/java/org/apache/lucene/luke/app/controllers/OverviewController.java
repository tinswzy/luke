package org.apache.lucene.luke.app.controllers;

import com.google.inject.Inject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.lucene.luke.app.controllers.dto.TermCount;
import org.apache.lucene.luke.app.controllers.dto.TopTerm;
import org.apache.lucene.luke.app.util.IntegerTextFormatter;
import org.apache.lucene.luke.models.LukeException;
import org.apache.lucene.luke.models.overview.Overview;
import org.apache.lucene.luke.models.overview.TermCounts;
import org.apache.lucene.luke.models.overview.TermStats;
import org.apache.lucene.luke.util.MessageUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.lucene.luke.app.util.ExceptionHandler.runnableWrapper;

public class OverviewController implements ChildController {

  @FXML
  private Label indexPath;

  @FXML
  private Label mode;

  @FXML
  private Label numFields;

  @FXML
  private Label numDocs;

  @FXML
  private Label numTerms;

  @FXML
  private Label delOpt;

  @FXML
  private Label indexVersion;

  @FXML
  private Label indexFormat;

  @FXML
  private Label dirImpl;

  @FXML
  private Label commitPoint;

  @FXML
  private Label userData;

  @FXML
  private TableView<TermCount> termCountTable;

  @FXML
  private TableColumn<TermCount, String> fieldColumn;

  @FXML
  private TableColumn<TermCount, Long> countColumn;

  @FXML
  private TableColumn<TermCount, Double> ratioColumn;

  private ObservableList<TermCount> termCountList;

  @FXML
  private TextField selectedField;

  @FXML
  private Button showTopTerms;

  @FXML
  private Spinner<Integer> numTopTerms;

  @FXML
  private TableView<TopTerm> topTermTable;

  @FXML
  private TableColumn<TopTerm, Integer> rankColumn;

  @FXML
  private TableColumn<TopTerm, Integer> freqColumn;

  @FXML
  private TableColumn<TopTerm, String> textColumn;

  private ObservableList<TopTerm> topTermList;

  @FXML
  private void initialize() {
    // initialize term counts table view
    fieldColumn.setCellValueFactory(new PropertyValueFactory<>("field"));
    countColumn.setCellValueFactory(new PropertyValueFactory<>("count"));
    ratioColumn.setCellValueFactory(new PropertyValueFactory<>("ratio"));
    termCountList = FXCollections.observableArrayList();
    termCountTable.setItems(termCountList);
    termCountTable.getSelectionModel().selectedIndexProperty().addListener((obs, oldv, newV) -> {
      TermCount selected = termCountTable.getSelectionModel().getSelectedItem();
      if (selected != null) {
        selectedField.setText(selected.getField());
        showTopTerms.setDisable(false);
      }
    });

    // initialize top terms table view
    rankColumn.setCellValueFactory(new PropertyValueFactory<>("rank"));
    freqColumn.setCellValueFactory(new PropertyValueFactory<>("freq"));
    textColumn.setCellValueFactory(new PropertyValueFactory<>("text"));
    topTermList = FXCollections.observableArrayList();
    topTermTable.setItems(topTermList);
    topTermTable.setContextMenu(createTopTermTableMenu());

    // initialize num of terms spinner
    SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory =
        new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 50, -1);
    numTopTerms.setValueFactory(valueFactory);
    TextFormatter<Integer> textFormatter = new IntegerTextFormatter(valueFactory.getConverter(), 50);
    valueFactory.valueProperty().bindBidirectional(textFormatter.valueProperty());
    numTopTerms.getEditor().setTextFormatter(textFormatter);
    numTopTerms.focusedProperty().addListener((obs, oldV, newV) -> {
      if (newV) {
        // won't not change value, but commit editor
        // https://stackoverflow.com/questions/32340476/manually-typing-in-text-in-javafx-spinner-is-not-updating-the-value-unless-user
        numTopTerms.increment(0);
      }
    });

    showTopTerms.setOnAction(e -> runnableWrapper(this::onShowTopTerms));
  }

  @Override
  public void onIndexOpen() throws LukeException {
    indexPath.setText(overviewModel.getIndexPath());
    if (parent.isReadOnly()) {
      mode.setText("(read-only)");
      ImageView imageView = new ImageView(new Image("/img/icon_lock.png"));
      imageView.setFitWidth(12);
      imageView.setFitHeight(12);
      mode.setGraphic(imageView);
      mode.setContentDisplay(ContentDisplay.RIGHT);
      Tooltip.install(mode, new Tooltip(MessageUtils.getLocalizedMessage("tooltip.read_only")));
    } else if (!parent.hasDirectoryReader()) {
      mode.setText("(multi-reader)");
      ImageView imageView = new ImageView(new Image("/img/icon_grid-2x2.png"));
      imageView.setFitWidth(12);
      imageView.setFitHeight(12);
      mode.setGraphic(imageView);
      mode.setContentDisplay(ContentDisplay.RIGHT);
      Tooltip.install(mode, new Tooltip(MessageUtils.getLocalizedMessage("tooltip.multi_reader")));
    } else {
      mode.setText("");
    }
    numFields.setText(overviewModel.getNumFields().toString());
    numDocs.setText(overviewModel.getNumDocuments().toString());
    numTerms.setText(overviewModel.getNumTerms().toString());
    String del = overviewModel.hasDeletions() ? String.format("Yes (%d)", overviewModel.getNumDeletedDocs()) : "No";
    String opt = overviewModel.isOptimized().map(b -> b ? "Yes" : "No").orElse("?");
    delOpt.setText(String.format("%s / %s", del, opt));
    indexVersion.setText(overviewModel.getIndexVersion().map(v -> Long.toString(v)).orElse("?"));
    indexFormat.setText(overviewModel.getIndexFormat());
    dirImpl.setText(overviewModel.getDirImpl());
    commitPoint.setText(overviewModel.getCommitDescription().orElse("---"));
    userData.setText(overviewModel.getCommitUserData().orElse("---"));

    // term counts
    double numTerms = overviewModel.getNumTerms().doubleValue();
    Map<String, Long> termCounts = overviewModel.getSortedTermCounts(TermCounts.Order.COUNT_DESC);
    termCountList.clear();
    termCountList.addAll(
        termCounts.entrySet()
            .stream()
            .map(e -> TermCount.of(e.getKey(), e.getValue(), numTerms))
            .collect(Collectors.toList())
    );

    showTopTerms.setDisable(true);
  }

  @Override
  public void onClose() {
    indexPath.setText("");
    numFields.setText("");
    numDocs.setText("");
    numTerms.setText("");
    delOpt.setText("");
    indexVersion.setText("");
    indexFormat.setText("");
    dirImpl.setText("");
    commitPoint.setText("");
    userData.setText("");
    termCountList.clear();
    selectedField.setText("");
    topTermList.clear();
  }

  @Override
  public void setParent(LukeController parent) {
    this.parent = parent;
  }

  private void onShowTopTerms() throws LukeException {
    String field = selectedField.getText();
    Integer numTerms = numTopTerms.getValue();
    List<TermStats> termStats = overviewModel.getTopTerms(field, numTerms);
    topTermList.clear();
    topTermList.addAll(
        IntStream.range(0, Math.min(numTerms, termStats.size()))
            .mapToObj(i -> TopTerm.of(i + 1, termStats.get(i)))
            .collect(Collectors.toList())
    );
    parent.clearStatusMessage();
  }

  private ContextMenu createTopTermTableMenu() {
    ContextMenu menu = new ContextMenu();

    // browse docs
    MenuItem item1 = new MenuItem(MessageUtils.getLocalizedMessage("overview.toptermtable.menu.item1"));
    item1.setOnAction(e -> runnableWrapper(() -> {
      TopTerm selected = topTermTable.getSelectionModel().getSelectedItem();
      parent.getDocumentsController().browseDocsByTerm(selectedField.getText(), selected.getText());
      parent.switchTab(LukeController.Tab.DOCUMENTS);
    }));

    // search docs
    MenuItem item2 = new MenuItem(MessageUtils.getLocalizedMessage("overview.toptermtable.menu.item2"));
    item2.setOnAction(e -> runnableWrapper(() -> {
      TopTerm selected = topTermTable.getSelectionModel().getSelectedItem();
      parent.getSearchController().searchByTerm(selectedField.getText(), selected.getText());
      parent.switchTab(LukeController.Tab.SEARCH);
    }));

    menu.getItems().addAll(item1, item2);
    return menu;
  }

  private Overview overviewModel;

  private LukeController parent;

  @Inject
  public OverviewController(Overview overviewModel) {
    this.overviewModel = overviewModel;
  }

}
