package ru.nlp_project.story_line2.glr_parser_testing;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class MarkupFileTest {

  private MarkupFile testable;

  @Before
  public void setUp() throws Exception {
  }

  @Test
  public void testExtractFactsFromMarkup() {
    testable = new MarkupFile(null);
    testable.html =
        "<html dir=\"ltr\"><head></head><body contenteditable=\"true\">Президент <span style=\"background-color: yellow;\">России Дмитрий Медведев</span> заявил, что считает \"более простым и реалистичным\" <span style=\"background-color: yellow;\">самостоятельное</span> вступление РФ в ВТО.</body></html>";
    testable.extractFacts();
    assertEquals(2, testable.facts.size());
    assertEquals("России Дмитрий Медведев", testable.facts.get(0).value);
    assertEquals("TEMPORAL", testable.facts.get(0).type);
    assertEquals("самостоятельное", testable.facts.get(1).value);
    assertEquals("TEMPORAL", testable.facts.get(1).type);
  }

}
