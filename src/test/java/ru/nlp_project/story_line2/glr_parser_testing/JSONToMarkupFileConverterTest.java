package ru.nlp_project.story_line2.glr_parser_testing;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;

import ru.nlp_project.story_line2.glr_parser_testing.JSONToMarkupFileConverter.IMarkupFileConsumer;

public class JSONToMarkupFileConverterTest {

  private JSONToMarkupFileConverter testable;

  @Before
  public void setUp() throws Exception {
    testable = new JSONToMarkupFileConverter();
  }

  @Test
  public void testConvertJSON() throws JsonParseException, IOException {
    List<MarkupFile> files = new ArrayList<>();
    class MarkupFileConsumerImpl implements IMarkupFileConsumer {
      @Override
      public void receveMarkupFile(MarkupFile file) {
        files.add(file);
      }
    }
    MarkupFileConsumerImpl consumer = new MarkupFileConsumerImpl();
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(
            "ru/nlp_project/story_line2/glr_parser_testing/politic-utf8-test.json");
    testable.readDB(stream, consumer);
    assertEquals(2, files.size());
    assertEquals("В субботу, 11 июля, на втором общероссийском съезде Движения против нелегальной иммиграции (ДПНИ) были приняты поправки к уставу организации, которые утверждают Национальный совет, состоящий из восьми человек, в качестве высшего руководящего органа ДПНИ. "
        + "Об этом сообщает \"Интерфакс\". "
        + "Ранее ДПНИ руководил один человек - Александр Белов (его должность называлась \"координатор Центрального совета\"). "
        + "Однако в мае, когда его признали виновным в разжигании межнациональной розни и приговорили к условному заключению сроком на полтора года, Белов объявил, что слагает с себя полномочия. "
        + "В настоящее время Белов является рядовым членом движения. "
        + "\"Самое важное, что ДПНИ переродилось в настоящую самостоятельную организацию. Теперь вне зависимости от того, есть или нет Белов, ДПНИ будет развиваться\", - сказал бывший лидер организации. "
        + "На съезде были избраны члены Национального совета. "
        + "По сообщению РИА Новости, ими стали делегаты из Волгограда, Кирова, Москвы, Нижнего Новгорода и Санкт-Петербурга. Согласно уставу ДПНИ, совет избирается сроком на три года. Ранее СМИ сообщали, что в состав совета войдут не восемь, а семь человек. "
        + "Также на съезде, проходившем в гостинице \"Измайлово\" в Москве, были избраны пять человек, которые войдут в так называемый Суд чести - орган, который призван решать спорные вопросы, возникающие внутри движения. "
        + "ДПНИ было создано в 2002 году. Целью организации объявлено искоренение нелегальной иммиграции в России, для ее достижения ДПНИ обещает использовать \"любые законные средства и методы\".", files.get(0).text);
    assertEquals("2009-07-11", files.get(0).publish);
  }

}
