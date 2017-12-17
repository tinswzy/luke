package org.apache.lucene.luke.app.controllers.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.apache.lucene.search.Explanation;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.IntStream;

public class ExplanationController implements DialogWindowController {

  @FXML
  private Label docNum;

  @FXML
  private TreeView<String> tree;

  @FXML
  private Button copy;

  @FXML
  private Button close;

  @FXML
  private void initialize() {
    copy.setOnAction(e -> copyToClipboard());
    close.setOnAction(e -> closeWindow(close));
  }

  public void setDocNum(int docId) {
    docNum.setText(String.valueOf(docId));
  }

  public void setExplanation(@Nonnull Explanation explanation) {
    TreeItem<String> root = createItem(explanation);
    traverse(root, explanation.getDetails());
    tree.setRoot(root);
  }

  private void traverse(TreeItem<String> parent, Explanation[] explanations) {
    parent.setExpanded(true);
    for (Explanation explanation : explanations) {
      TreeItem<String> item = createItem(explanation);
      parent.getChildren().add(item);
      traverse(item, explanation.getDetails());
    }
  }

  private TreeItem<String> createItem(Explanation explanation) {
    return new TreeItem<>(String.format("%f  %s", explanation.getValue(), explanation.getDescription()));
  }

  private void copyToClipboard() {
    Clipboard clipboard = Clipboard.getSystemClipboard();
    ClipboardContent content = new ClipboardContent();
    content.putString(treeToString());
    clipboard.setContent(content);
  }

  private String treeToString() {
    TreeItem<String> root = tree.getRoot();
    StringBuilder sb = new StringBuilder(root.getValue());
    sb.append("\n");
    traverseToCopy(sb, 1, root.getChildren());
    return sb.toString();
  }

  private void traverseToCopy(StringBuilder sb, int depth, List<TreeItem<String>> items) {
    for (TreeItem<String> item : items) {
      IntStream.range(0, depth).forEach(i -> sb.append("  "));
      sb.append(item.getValue());
      sb.append("\n");
      traverseToCopy(sb, depth + 1, item.getChildren());
    }
  }

}
