package org.woped.core.utilities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class LlmModelFilterTest {

  @Test
  public void isSupported_openAiChatModels_returnTrue() {
    assertTrue(LlmModelFilter.isSupported("gpt-4o-mini", "openAi"));
    assertTrue(LlmModelFilter.isSupported("gpt-3.5-turbo", "openAi"));
    assertTrue(LlmModelFilter.isSupported("gpt-4-turbo", "openAi"));
    assertTrue(LlmModelFilter.isSupported("o1-mini", "openAi"));
  }

  @Test
  public void isSupported_openAiNonChatModels_returnFalse() {
    assertFalse(LlmModelFilter.isSupported("gpt-4o-mini-transcribe-2025-03-20", "openAi"));
    assertFalse(LlmModelFilter.isSupported("text-embedding-3-small", "openAi"));
    assertFalse(LlmModelFilter.isSupported("whisper-1", "openAi"));
    assertFalse(LlmModelFilter.isSupported("dall-e-3", "openAi"));
  }

  @Test
  public void isSupported_geminiChatModels_returnTrue() {
    assertTrue(LlmModelFilter.isSupported("gemini-2.0-flash", "gemini"));
    assertTrue(LlmModelFilter.isSupported("gemini-1.5-pro", "gemini"));
  }

  @Test
  public void isSupported_geminiNonChatModels_returnFalse() {
    assertFalse(LlmModelFilter.isSupported("embedding-001", "gemini"));
    assertFalse(LlmModelFilter.isSupported("imagen-3.0-generate-001", "gemini"));
  }

  @Test
  public void filterForTextGeneration_removesUnsupportedModels() {
    List<String> input =
        Arrays.asList(
            "gpt-4o-mini",
            "gpt-4o-mini-transcribe-2025-03-20",
            "text-embedding-3-small",
            "gemini-2.0-flash",
            "embedding-001");
    List<String> openAiFiltered = LlmModelFilter.filterForTextGeneration(input, "openAi");
    assertEquals(1, openAiFiltered.size());
    assertEquals("gpt-4o-mini", openAiFiltered.get(0));

    List<String> geminiFiltered = LlmModelFilter.filterForTextGeneration(input, "gemini");
    assertEquals(1, geminiFiltered.size());
    assertEquals("gemini-2.0-flash", geminiFiltered.get(0));
  }

  @Test
  public void resolveSelection_prefersSupportedSavedModel() {
    List<String> models = Arrays.asList("gpt-4o-mini", "gpt-4o");
    assertEquals("gpt-4o-mini", LlmModelFilter.resolveSelection(models, "gpt-4o-mini", "openAi"));
  }

  @Test
  public void resolveSelection_fallsBackWhenSavedModelUnsupported() {
    List<String> models = Arrays.asList("gpt-4o-mini", "gpt-4o");
    assertEquals(
        "gpt-4o-mini",
        LlmModelFilter.resolveSelection(models, "gpt-4o-mini-transcribe-2025-03-20", "openAi"));
  }
}
