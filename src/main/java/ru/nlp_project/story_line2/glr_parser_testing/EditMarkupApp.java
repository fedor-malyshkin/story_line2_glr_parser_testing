package ru.nlp_project.story_line2.glr_parser_testing;

import java.io.File;
import java.net.URL;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class EditMarkupApp extends Application {

  private static final String STAGE_CAPTION = "Markup File Editor";

  public static void main(String[] args) {
    launch(args);
  }

  private Pane rootPane;
  private Stage primaryStage;
  private MarkupFile selectedFile;
  private HTMLEditor htmlEditorCtrl;

  String selectLocationScript =
      "var range = window.getSelection().getRangeAt(0);"
          + "var t = range.toString();"
          + "range.extractContents();"
          + "var span = document.createElement(\"span\");"
          + "span.style.backgroundColor = \"blue\";"
          + "span.innerHTML = t;" + "range.insertNode(span);";

  String selectTemporaryScript =
      "var range = window.getSelection().getRangeAt(0);"
          + "var t = range.toString();"
          + "range.extractContents();"
          + "var span = document.createElement(\"span\");"
          + "span.style.backgroundColor = \"yellow\";"
          + "span.innerHTML = t;" + "range.insertNode(span);";

  String resetSelectedScript =
      "var range = window.getSelection().getRangeAt(0);"
          + "var t = range.toString();"
          + "range.extractContents();"
          + "var tn = document.createTextNode(t);"
          + "range.insertNode(tn);";

  private void initializeComponents() {
    htmlEditorCtrl = (HTMLEditor) rootPane.lookup("#htmlEditorCtrl");
    Button openBtnCtrl = (Button) rootPane.lookup("#openBtnCtrl");
    Button saveBtnCtrl = (Button) rootPane.lookup("#saveBtnCtrl");
    Button resetBtnCtrl = (Button) rootPane.lookup("#resetBtnCtrl");

    openBtnCtrl.setOnAction(e -> {
      openAction();
    });

    saveBtnCtrl.setOnAction(e -> {
      saveAction();
    });

    resetBtnCtrl.setOnAction(e -> {
      resetWholeDocument();
    });

    htmlEditorCtrl.addEventFilter(KeyEvent.ANY, new EventHandler<KeyEvent>() {
      @Override
      public void handle(KeyEvent ke) {
        if (ke.isControlDown() && ke.getCode() == KeyCode.O)
          openAction();
        if (ke.isControlDown() && ke.getCode() == KeyCode.S)
          saveAction();
        if (ke.isControlDown() && ke.getCode() == KeyCode.T)
          selectTemporary();
        if (ke.isControlDown() && ke.getCode() == KeyCode.L)
          selectLocation();
        if (ke.isControlDown() && ke.getCode() == KeyCode.R)
          resetSelected();
        ke.consume();
      }
    });

    rootPane.setOnKeyPressed(new EventHandler<KeyEvent>() {
      public void handle(KeyEvent ke) {
        if (ke.isControlDown() && ke.getCode() == KeyCode.O)
          openAction();
        if (ke.isControlDown() && ke.getCode() == KeyCode.S)
          saveAction();
        if (ke.isControlDown() && ke.getCode() == KeyCode.T)
          selectTemporary();
        if (ke.isControlDown() && ke.getCode() == KeyCode.L)
          selectLocation();
        if (ke.isControlDown() && ke.getCode() == KeyCode.R)
          resetSelected();
      }
    });
    
    htmlEditorCtrl.setHtmlText("<html><body><p>Open Markup JSON file for start...</p>"
        + "Hotkeys:"
        + "<ol>"
        + "<li>Ctrl + O - Open file</li>"
        + "<li>Ctrl + S - Save file</li>"
        + "<li>Ctrl + T - Mark selected text as TEMPORAL</li>"
        + "<li>Ctrl + L - Mark selected text as LOCATION</li>"
        + "<li>Ctrl + R - Reset selected text</li>"
+ "</ol></body></html>");
  }

  private void loadFile(File file) {
    selectedFile = MarkupFile.loadFromFile(file);
    if (selectedFile == null) {
      htmlEditorCtrl.setHtmlText("<html><body>Ошибка при открытии файла: <b>"
          + file.getAbsolutePath() + "</b></body></html>");
      return;
    }
    htmlEditorCtrl.setHtmlText(selectedFile.html);
    primaryStage.setTitle(STAGE_CAPTION + " - " + file.getName());
  }

  private void openAction() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle(STAGE_CAPTION);
    if (selectedFile != null)
      chooser.setInitialDirectory(selectedFile.file.getParentFile());
    else
      chooser.setInitialDirectory(new File("data/economy-markup"));

    File file = chooser.showOpenDialog(primaryStage);
    if (file != null)
      loadFile(file);
  }

  protected void resetSelected() {
    if (selectedFile == null)
      return;
    WebView webView = (WebView) htmlEditorCtrl.lookup("WebView");
    if (webView != null) {
      WebEngine engine = webView.getEngine();
      engine.executeScript(resetSelectedScript);
    }
  }

  protected void resetWholeDocument() {
    if (selectedFile == null)
      return;
    htmlEditorCtrl.setHtmlText(selectedFile.text);
  }

  private void saveAction() {
    if (selectedFile == null)
      return;

    String htmlText = htmlEditorCtrl.getHtmlText();
    selectedFile.html = htmlText;
    selectedFile.filterOutHtml();
    selectedFile.extractFacts();
    selectedFile.saveToFile();
  }

  protected void selectLocation() {
    if (selectedFile == null)
      return;
    WebView webView = (WebView) htmlEditorCtrl.lookup("WebView");
    if (webView != null) {
      WebEngine engine = webView.getEngine();
      engine.executeScript(selectLocationScript);
    }
  }

  protected void selectTemporary() {
    if (selectedFile == null)
      return;
    WebView webView = (WebView) htmlEditorCtrl.lookup("WebView");
    if (webView != null) {
      WebEngine engine = webView.getEngine();
      engine.executeScript(selectTemporaryScript);
    }
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    URL fxml = Thread.currentThread().getContextClassLoader().getResource(
        "ru/nlp_project/story_line2/glr_parser_testing/fxml/mainForm.fxml");
    rootPane = FXMLLoader.load(fxml);
    rootPane.getStylesheets()
        .add("ru/nlp_project/story_line2/glr_parser_testing/css/javafx.css");
    this.primaryStage = primaryStage;
    primaryStage.setScene(new Scene(rootPane, 800, 600));
    // primaryStage.setMaximized(true);
    primaryStage.setTitle(STAGE_CAPTION);
    primaryStage.show();
    primaryStage.setOnCloseRequest(e -> Platform.exit());
    initializeComponents();
  }

}
