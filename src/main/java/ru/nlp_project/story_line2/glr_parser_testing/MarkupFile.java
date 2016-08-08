package ru.nlp_project.story_line2.glr_parser_testing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.databind.ObjectMapper;

class MarkupFile {
  public final static String FACT_TYPE_TEMPORAL = "TEMPORAL";
  public final static String FACT_TYPE_PERSON = "PERSON";
  public final static String FACT_TYPE_LOCATION = "LOCATION";
  public File file;

  public MarkupFile(File file) {
    this.file = file;
  }

  public MarkupFile(String html, String text) {
    super();
    this.html = html;
    this.text = text;
  }

  public static class ExtractedFact {
    String type;
    String value;
  }

  String html;
  String publish;
  String text;
  List<ExtractedFact> facts = new ArrayList<ExtractedFact>();

  @Override
  public String toString() {
    return String.format("MarkupFile [html=%s, text=%s, facts=%s]", html, text,
        facts);
  }

  public Map<String, Object> convertToMap() {
    HashMap<String, Object> result = new HashMap<>();
    result.put("html", html);
    result.put("text", text);
    result.put("publish", publish);
    List<HashMap<String, Object>> list = facts.stream().map(f -> {
      HashMap<String, Object> res = new HashMap<>();
      res.put("type", f.type);
      res.put("value", f.value);
      return res;
    }).collect(Collectors.toList());
    result.put("facts", list);
    return result;
  }

  @SuppressWarnings("unchecked")
  public static MarkupFile convertFromMap(File file, Map<String, Object> map) {
    MarkupFile result = new MarkupFile(file);
    result.html = (String) map.get("html");
    result.text = (String) map.get("text");
    result.publish = (String) map.get("publish");

    List<Map<String, Object>> facts =
        (List<Map<String, Object>>) map.get("facts");
    if (facts != null) {
      result.facts = facts.stream().map(f -> {
        ExtractedFact fo = new ExtractedFact();
        fo.value = (String) f.get("value");
        fo.type = (String) f.get("type");
        return fo;
      }).collect(Collectors.toList());
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  public static MarkupFile loadFromFile(File selectedFile) {
    try {
      FileInputStream fis = new FileInputStream(selectedFile);
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> map = mapper.readValue(fis, HashMap.class);
      IOUtils.closeQuietly(fis);
      return convertFromMap(selectedFile, map);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public void saveToFile() {
    try {
      if (!file.exists())
        file.createNewFile();
      FileOutputStream outputStream = new FileOutputStream(file);
      ObjectMapper mapper = new ObjectMapper();
      mapper.writeValue(outputStream, this.convertToMap());
      IOUtils.closeQuietly(outputStream);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void extractFacts() {
    facts.clear();
    Document doc = Jsoup.parse(html, "UTF-8");
    Elements spans = doc.select("span[style~=background-color.*yellow.*");
    for (Element snap : spans) {
      ExtractedFact extractedFact = new ExtractedFact();
      if (snap.text().trim().isEmpty())
        continue;
      extractedFact.value = snap.text();
      extractedFact.type = FACT_TYPE_TEMPORAL;
      facts.add(extractedFact);
    }
    spans = doc.select("span[style~=background-color.*blue.*");
    for (Element snap : spans) {
      ExtractedFact extractedFact = new ExtractedFact();
      if (snap.text().trim().isEmpty())
        continue;
      extractedFact.value = snap.text();
      extractedFact.type = FACT_TYPE_LOCATION;
      facts.add(extractedFact);
    }
  }

  public void filterOutHtml() {
    Document doc = Jsoup.parse(html, "UTF-8");
    Elements spans = doc.select("span");
    for (Element snap : spans) {
      if (snap.text().isEmpty())
        snap.remove();
    }
    html = doc.html();
  }

}
