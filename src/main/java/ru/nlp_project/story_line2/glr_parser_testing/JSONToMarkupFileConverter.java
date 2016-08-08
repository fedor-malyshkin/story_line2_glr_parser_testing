package ru.nlp_project.story_line2.glr_parser_testing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Осуществляет конвертацию БОЛЬШОГО JSON файла в {@link MarkupFile},
 * осуществляее необходимую "чистку" файла.
 * 
 * @author fedor
 *
 */
public class JSONToMarkupFileConverter {
  public final static int LIMIT = 100;

  public static class MarkupFileConsumerImpl implements IMarkupFileConsumer {

    private File outputDir;
    private int counter;

    public MarkupFileConsumerImpl(File outputDir) {
      this.outputDir = outputDir;
      counter = 0;
    }

    @Override
    public void receveMarkupFile(MarkupFile file) {
      File outputFile = new File(outputDir, String.format("%02d.json", counter));
      file.file = outputFile;
      file.saveToFile();
      counter++;
    }

    public static MarkupFileConsumerImpl newInstance() throws IOException {
      File outputDir = createMarkupFileDirectory();
      return new MarkupFileConsumerImpl(outputDir);
    }
  }

  private static final String JSON_FILE = "economy-utf8.json";

  public static void main(String[] args)
      throws JsonParseException, IOException {
    JSONToMarkupFileConverter instance = new JSONToMarkupFileConverter();
    instance.run();
  }

  private JsonFactory jsonFactory;
  private JsonParser jp;

  interface IMarkupFileConsumer {
    void receveMarkupFile(MarkupFile file);
  }

  void run() throws JsonParseException, IOException {
    IMarkupFileConsumer consumer = MarkupFileConsumerImpl.newInstance();
    readDB(new FileInputStream("data/" + JSON_FILE), consumer);
  }

  protected void readDB(InputStream inputStream, IMarkupFileConsumer consumer)
      throws JsonParseException, IOException {
    if (inputStream == null)
      throw new IllegalArgumentException();
    jsonFactory = new JsonFactory(); // or, for data binding,
    jp = jsonFactory.createParser(inputStream);
    readContent(jp, consumer);
    jp.close();
    IOUtils.closeQuietly(inputStream);
  }

  private void readContent(JsonParser jp, IMarkupFileConsumer consumer)
      throws IOException {
    int fileCounter = 0;
    while (jp.getCurrentToken() != JsonToken.END_ARRAY) {
      // field("es"),
      JsonToken currToken = jp.nextToken();
      if (currToken == JsonToken.END_ARRAY)
        return;
      while (currToken != JsonToken.START_OBJECT || currToken == null)
        currToken = jp.nextToken();

      // publish
      while ((currToken != JsonToken.FIELD_NAME)
          || !(currToken == JsonToken.FIELD_NAME
              && jp.getText().equalsIgnoreCase("publish"))
          || currToken == null)
        currToken = jp.nextToken();

      jp.nextValue();
      String publish = jp.getText();

      // text
      while ((currToken != JsonToken.FIELD_NAME)
          || !(currToken == JsonToken.FIELD_NAME
              && jp.getText().equalsIgnoreCase("text"))
          || currToken == null)
        currToken = jp.nextToken();

      // filed "text" -> to value
      jp.nextValue();
      String text = jp.getText();
      processMarkupFile(text, publish, consumer);
      if (++fileCounter >= LIMIT)
        return;

      jp.nextToken();
      while (jp.nextToken() != JsonToken.END_OBJECT)
        ;
    }
  }

  private void processMarkupFile(String text, String publish,
      IMarkupFileConsumer consumer) throws IOException {
    text = text.replaceAll("\\n", " ");
    text = Jsoup.parse(text).text();
    // List<String> strings = TokenManager.splitIntoStrings(text);
    StringBuffer sb = new StringBuffer("<html><body>");
    /*
     * String string = strings.stream().map(s -> "<span>" + s + "</span>")
     * .collect(Collectors.joining(" "));
     */
    sb.append(text + "</body></html>");
    MarkupFile markupFile = new MarkupFile(sb.toString(), text);
    markupFile.publish = publish;
    consumer.receveMarkupFile(markupFile);
  }

  private static File createMarkupFileDirectory() throws IOException {
    File file = new File("data/" + JSON_FILE + "-markup");
    file.mkdir();
    return file;
  }
}
