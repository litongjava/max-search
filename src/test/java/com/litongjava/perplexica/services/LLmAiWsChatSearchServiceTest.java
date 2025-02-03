package com.litongjava.perplexica.services;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.jfinal.kit.Kv;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.model.web.WebPageContent;
import com.litongjava.template.PromptEngine;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.json.JsonUtils;

public class LLmAiWsChatSearchServiceTest {

  @Test
  public void testGenerateSelectPrompt() {
    String question = "When is the first day of sjsu";
    List<WebPageContent> results = new ArrayList<>();
    results.add(new WebPageContent("Academic-Calendar-2024-25.pdf", "https://www.sjsu.edu/provost/docs/Academic-Calendar-2024-25.pdf",
        //
        "First Day of Instruction – Classes Begin. Tuesday………………… February 18… ... SJSU Academic Year Calendar. 2024/25-Draft. October 24, 2023. Fall."));

    results.add(new WebPageContent("2024-2025 | Class Schedules", "https://www.sjsu.edu/classes/calendar/2024-2025.php",
        //
        "First Day of Instruction, August 21, 2024, January 23, 2025 ; Enrollment Census Date*, September 18, 2024, February 19, 2025 ; Last Day of Instruction, December 9."));

    Kv kv = Kv.by("quesiton", question).set("search_result", JsonUtils.toJson(results));
    String fileName = "WebSearchSelectPrompt.txt";
    String prompt = PromptEngine.renderToString(fileName, kv);
    System.out.println(prompt);
  }

  @Test
  public void testSearchAndPredict() {
    EnvUtils.load();
    String content = "Advertising, Area of Specialization in Creative Track, BS (2024-2025) 4 year";
    Aop.get(WsChatService.class).google(null, null, null, content);
  }

}
