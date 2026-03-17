package org.woped.file.t2p;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.woped.file.t2p.model.LlmResponse;

final class T2PResponseParser {
    private static final Gson GSON = new Gson();

    private T2PResponseParser() {
    }

    static String extractPnml(String json) throws JsonSyntaxException {
        if (json == null) {
            return null;
        }

        LlmResponse response = GSON.fromJson(json, LlmResponse.class);
        if (response == null) {
            return null;
        }

        return response.getResult();
    }
}
