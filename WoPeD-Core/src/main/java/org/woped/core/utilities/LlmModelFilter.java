package org.woped.core.utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Filters provider model lists to models suitable for P2T/T2P text generation
 * (chat / generateContent), using a hybrid whitelist + blacklist strategy.
 */
public final class LlmModelFilter {

  private static final String PROVIDER_OPENAI = "openAi";
  private static final String PROVIDER_GEMINI = "gemini";
  private static final String PROVIDER_LMSTUDIO = "lmStudio";

  private static final String DEFAULT_OPENAI_MODEL = "gpt-4o-mini";
  private static final String DEFAULT_GEMINI_MODEL = "gemini-2.0-flash";

  private static final String[] OPENAI_WHITELIST_PREFIXES = {
    "gpt-3.5-turbo",
    "gpt-4.1",
    "gpt-4o",
    "gpt-4",
    "gpt-5",
    "o4-",
    "o4",
    "o3-",
    "o3",
    "o1-",
    "o1",
    "chatgpt-"
  };

  private static final String[] OPENAI_BLACKLIST_FRAGMENTS = {
    "transcribe",
    "whisper",
    "tts",
    "embedding",
    "dall-e",
    "moderation",
    "realtime",
    "audio",
    "gpt-image",
    "davinci",
    "babbage",
    "curie",
    "ada",
    "instruct",
    "codex",
    "search-api",
    "text-davinci",
    "ft:",
    "omni-moderation"
  };

  private static final String[] GEMINI_WHITELIST_PREFIXES = {
    "gemini-",
    "gemini"
  };

  private static final String[] GEMINI_BLACKLIST_FRAGMENTS = {
    "embedding",
    "imagen",
    "veo",
    "aqa",
    "tuner",
    "preview-tts",
    "live-"
  };

  private LlmModelFilter() {}

  /**
   * Keeps only models that are likely to work with chat / text generation APIs.
   * LM Studio models are returned unchanged.
   */
  public static List<String> filterForTextGeneration(List<String> models, String provider) {
    if (models == null || models.isEmpty()) {
      return Collections.emptyList();
    }
    if (provider == null || provider.isEmpty()) {
      provider = PROVIDER_OPENAI;
    }
    if (PROVIDER_LMSTUDIO.equals(provider)) {
      return new ArrayList<>(models);
    }

    List<String> filtered = new ArrayList<>();
    for (String model : models) {
      if (model == null || model.isBlank()) {
        continue;
      }
      if (isSupported(model.trim(), provider)) {
        filtered.add(model.trim());
      }
    }
    Collections.sort(filtered);
    return filtered;
  }

  public static boolean isSupported(String modelId, String provider) {
    if (modelId == null || modelId.isBlank()) {
      return false;
    }
    if (provider == null || provider.isEmpty()) {
      provider = PROVIDER_OPENAI;
    }
    if (PROVIDER_LMSTUDIO.equals(provider)) {
      return true;
    }

    String normalized = modelId.trim().toLowerCase(Locale.ROOT);
    if (PROVIDER_GEMINI.equals(provider)) {
      return matchesWhitelist(normalized, GEMINI_WHITELIST_PREFIXES)
          && !containsAnyFragment(normalized, GEMINI_BLACKLIST_FRAGMENTS);
    }
    if (PROVIDER_OPENAI.equals(provider)) {
      return matchesWhitelist(normalized, OPENAI_WHITELIST_PREFIXES)
          && !containsAnyFragment(normalized, OPENAI_BLACKLIST_FRAGMENTS);
    }
    return true;
  }

  public static String getDefaultModel(String provider) {
    if (PROVIDER_GEMINI.equals(provider)) {
      return DEFAULT_GEMINI_MODEL;
    }
    return DEFAULT_OPENAI_MODEL;
  }

  /**
   * Picks the preferred model when still supported; otherwise the default or first filtered entry.
   */
  public static String resolveSelection(List<String> filteredModels, String preferred, String provider) {
    if (filteredModels == null || filteredModels.isEmpty()) {
      return getDefaultModel(provider);
    }
    if (preferred != null && !preferred.isBlank()) {
      String trimmed = preferred.trim();
      if (filteredModels.contains(trimmed) && isSupported(trimmed, provider)) {
        return trimmed;
      }
    }
    String defaultModel = getDefaultModel(provider);
    if (filteredModels.contains(defaultModel)) {
      return defaultModel;
    }
    return filteredModels.get(0);
  }

  private static boolean matchesWhitelist(String modelId, String[] prefixes) {
    for (String prefix : prefixes) {
      if (modelId.equals(prefix) || modelId.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  private static boolean containsAnyFragment(String modelId, String[] fragments) {
    for (String fragment : fragments) {
      if (modelId.contains(fragment)) {
        return true;
      }
    }
    return false;
  }

  /** Exposed for tests and documentation. */
  public static List<String> getOpenAiWhitelistPrefixes() {
    return Collections.unmodifiableList(Arrays.asList(OPENAI_WHITELIST_PREFIXES));
  }

  public static List<String> getOpenAiBlacklistFragments() {
    return Collections.unmodifiableList(Arrays.asList(OPENAI_BLACKLIST_FRAGMENTS));
  }

  public static List<String> getGeminiWhitelistPrefixes() {
    return Collections.unmodifiableList(Arrays.asList(GEMINI_WHITELIST_PREFIXES));
  }

  public static List<String> getGeminiBlacklistFragments() {
    return Collections.unmodifiableList(Arrays.asList(GEMINI_BLACKLIST_FRAGMENTS));
  }
}
