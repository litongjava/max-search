package com.litongjava.perplexica.utils;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.litongjava.tio.utils.json.FastJson2Utils;

public class OpenAiResponseUtils {

  public static void extraChoices(StringBuffer complectionContent, StringBuffer fnCallName, StringBuffer fnCallArgs,
      JSONArray choices) {
    if (choices.size() > 0) {
      for (int i = 0; i < choices.size(); i++) {
        JSONObject delta = choices.getJSONObject(i).getJSONObject("delta");
        String part = delta.getString("content");
        if (part != null) {
          complectionContent.append(part);
        }
        String functionCallString = delta.getString("function_call");
        if (functionCallString != null) {
          JSONObject functionCall = FastJson2Utils.parseObject(functionCallString);
          String name = functionCall.getString("name");
          if (name != null) {
            fnCallName.append(name);
          }

          String arguments = functionCall.getString("arguments");
          if (arguments != null) {
            // System.out.println("arguments:" + arguments);
            fnCallArgs.append(arguments);
          }
        }
      }
    }
  }
}
