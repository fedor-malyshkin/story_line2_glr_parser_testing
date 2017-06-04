package ru.nlp_project.story_line2.glr_parser_testing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.collections4.Factory;
import org.apache.commons.collections4.map.LazyMap;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import ru.nlp_project.story_line2.glr_parser.GLRParser;
import ru.nlp_project.story_line2.glr_parser.IFactListener;
import ru.nlp_project.story_line2.glr_parser.IGLRLogger;
import ru.nlp_project.story_line2.glr_parser.SentenceProcessingContext;
import ru.nlp_project.story_line2.glr_parser_testing.MarkupFile.ExtractedFact;

/**
 * Класс для расчета  результатов анализа текстовых данных разработанными 
 * скриптами.
 * <br/>
 * Входные данные: JSON  файлы с предварительно исвлеченными фактами({@link MarkupFile}}), 
 * скпиты анализатора. Анализу подвергаются все факты имени TEMPORAL (с полями 
 * TEMPORAL.xxxx), LOCATION, PERSON и ENTITY.
 * <br/>
 * Для достижения результатав всем директивам интерпретатора добавляется помета
 * "<no-term>", сверка осуществляется после удаления излишних пробелов и 
 * приведения к нижнему регистру.
 * <br/>
 * Исходные данные: длоги анализа и расчет следующих показателей:
 * <ol>
 * <li><a href="https://en.wikipedia.org/wiki/Precision_and_recall">precision</a></li>
 * <li><a href="https://en.wikipedia.org/wiki/Precision_and_recall">recall</a></li>
 * <li><a href="https://en.wikipedia.org/wiki/F1_score">F! Score</a></li>
 * <li><a href="https://en.wikipedia.org/wiki/Accuracy_and_precision">Accuracy</a></li>
 * </ol>
 * 
 * @author fedor
 *
 */
public class AnalysisResultCalculator {
  class Fact {
    String name, field, value;

    public Fact(String name, String field, String value) {
      super();
      this.name = name;
      this.field = field;
      this.value = value;
    }

    @Override
    public String toString() {
      return String.format("{%s.%s=%s}", name, field, value);
    }

  }

  List<String> notDetectedTemporals =
      Collections.synchronizedList(new ArrayList<>());

  class FactListener implements IFactListener {
    // т.к. может вызываться на дополнение из разных потоков
    List<Fact> facts = Collections.synchronizedList(new ArrayList<>());

    // . может вызываться из разных потоков аналлизатора
    @Override
    public void factExtracted(SentenceProcessingContext context,
        ru.nlp_project.story_line2.glr_parser.InterpreterImpl.Fact fact) {
      fact.getFieldsMap().entrySet().stream().forEach(e -> {
        if (e.getValue().getValue() != null
            && !e.getValue().getValue().isEmpty())
          facts.add(new Fact(fact.getName(), e.getValue().getName(),
              normalizeValue(e.getValue().getValue())));
      });
    }

    public List<Fact> getFacts() {
      return facts;
    }

    public void reset() {
      facts.clear();
    }
  }

  class GLRLogger implements IGLRLogger {

    @Override
    public void error(String message, Exception e) {
      log.error(message, e);
    }

  }

  private static final String CONFIG_ANLYSER = "temporal/glr-config.json";
  private static final String CONFIG_MARKUP_DIR = "data/economy-markup";

  public static void main(String[] args) {
    System.setProperty("logback.configurationFile", "config/logback.xml");
    AnalysisResultCalculator instance = new AnalysisResultCalculator();
    instance.run();
  }

  private org.slf4j.Logger log;

  private GLRLogger logger;
  private FactListener factListener;
  private GLRParser analyser;
  private ArrayList<File> files;
  private float temporalFP;
  private float temporalFN;
  private float temporalTP;

  public AnalysisResultCalculator() {
    log = LoggerFactory.getLogger(this.getClass());
  }

  private void analyse() throws IOException {
    resetGlobalStatistics();
    int counter = 1;
    Collections.sort(files, new Comparator<File>() {
      @Override
      public int compare(File o1, File o2) {
        return o1.getName().compareTo(o2.getName());
      }
    });
    for (File f : files) {
      log.debug("Process file: " + f.getName());
      log.info("Left files: " + (files.size() - counter));
      MarkupFile markupFile = MarkupFile.loadFromFile(f);
      factListener.reset();
      long start = System.currentTimeMillis();
      resetLocalStatistics();
      analyser.processText(markupFile.text);
      updateStatistics(f, markupFile.facts, factListener.getFacts());
      long end = System.currentTimeMillis();
      log.debug(String.format("File '%s' analysed: %d ms.", f.getName(),
          (end - start)));
      counter++;
    }

  }

  private void resetLocalStatistics() {
    factListener.facts.clear();
  }

  private void initAnalyser() throws IOException {
    long start = System.currentTimeMillis();
    logger = new GLRLogger();
    factListener = new FactListener();

    analyser = GLRParser.newInstance("config/" + CONFIG_ANLYSER, logger,
        factListener, true, false);

    long end = System.currentTimeMillis();
    log.info(String.format("Analyser started: %d ms.", end - start));
  }

  private String normalizeValue(String value) {
    String result = value.toLowerCase();
    result = result.replaceAll("-", " - ");
    result = result.replaceAll(",", " , ");
    result = result.replaceAll("\\s+", " ");
    return result.trim();
  }

  private void printSummary() {
    float recall = temporalTP > 0 ? temporalTP / (temporalTP + temporalFN) : 0;
    float precision =
        temporalTP > 0 ? temporalTP / (temporalTP + temporalFP) : 0;
    float f1 = 2 * (recall * precision) / (recall + precision);
    log.info(String.format(
        "Whole statistics: recall: %.2f, precision: %.2f, f1 score: %.2f",
        recall, precision, f1));

    // заполнить массив - ключь:кол-во вхождений
    Map<String, Integer> notCollectedStringCount =
        LazyMap.lazyMap(new HashMap<String, Integer>(), new Factory<Integer>() {
          @Override
          public Integer create() {
            return new Integer(0);
          }

        });
    notDetectedTemporals.stream().forEach(t -> {
      int count = notCollectedStringCount.get(t);
      notCollectedStringCount.put(t, ++count);
    });
    // отсортировать в уменьшающемся порядке и сеарилизовать
    String strings = notCollectedStringCount.entrySet().stream()
        .sorted(new Comparator<Map.Entry<String, Integer>>() {
          @Override
          public int compare(Entry<String, Integer> o1,
              Entry<String, Integer> o2) {
            return -1 * o1.getValue().compareTo(o2.getValue());
          }
        }).map(e -> "" + e.getValue() + " - " + e.getKey())
        .collect(Collectors.joining(";\n"));

    log.info(String.format("Not collected temporals: %s", strings));
  }

  private void readMarkupDirectory() {
    Collection<File> filesTemp = FileUtils
        .listFiles(new File(CONFIG_MARKUP_DIR), new String[] { "json" }, false);
    files = new ArrayList<File>(filesTemp);
  }

  private void resetGlobalStatistics() {
    notDetectedTemporals.clear();
    temporalTP = 0;
    temporalFN = 0;
    temporalFP = 0;

  }

  private void run() {
    try {

      initAnalyser();
      readMarkupDirectory();

      long start = System.currentTimeMillis();
      analyse();
      printSummary();
      long end = System.currentTimeMillis();
      log.info(String.format("Whole analysis took: %d ms.", end - start));

      analyser.shutdown();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  private void updateStatistics(File f, List<ExtractedFact> markupFacts,
      List<Fact> analysedFacts) {
    // true positive
    float tp = 0;
    // false positive
    float fp = 0;
    List<String> tp_temporal = new ArrayList<>();
    List<String> fp_temporal = new ArrayList<>();
    markupFacts.stream().forEach(fact -> {
      fact.value = normalizeValue(fact.value);
      if (fact.type.equals(MarkupFile.FACT_TYPE_TEMPORAL))
        tp_temporal.add(fact.value);
    });
    log.trace(String.format("File '%s' contains temporal: %s", f.getName(),
        tp_temporal.toString()));
    // false negative
    float fn = tp_temporal.size();
    // удаляем найденный, увеличивая счетчик
    for (Fact fact : analysedFacts) {
      if (!fact.name.equalsIgnoreCase(MarkupFile.FACT_TYPE_TEMPORAL))
        continue;

      if (tp_temporal.contains(fact.value)) {
        tp_temporal.remove(fact.value);
        tp++; // inc for matched temporal
      } else {
        fp_temporal.add(fact.value);
        fp++; // inc for every notmatched temporal
      }
    }
    log.trace(String.format(
        "Anlyser in file '%s' did'n found TRUE POSITIVE temporal: %s",
        f.getName(), tp_temporal.toString()));
    log.trace(
        String.format("Anlyser in file '%s' found FALSE POSITIVE temporal: %s",
            f.getName(), fp_temporal.toString()));
    // false positive correction
    fn -= tp;
    float recall = tp > 0 ? tp / (tp + fn) : 0;
    float precision = tp > 0 ? tp / (tp + fp) : 0;
    float f1 = 2 * (recall * precision) / (recall + precision);
    log.debug(String.format(
        "File '%s' statistics: recall: %.2f, precision: %.2f, f1 score: %.2f",
        f.getName(), recall, precision, f1));

    temporalTP += tp;
    temporalFN += fn;
    temporalFP += fp;
    notDetectedTemporals.addAll(tp_temporal);
    ;
  }
}
