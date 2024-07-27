package org.schabi.newpipe;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

import java.util.Optional;

public final class BraveNewVersionWorkerHelper {

    private BraveNewVersionWorkerHelper() {
    }

    public static JsonObject getVersionInfo(
            final String jsonData,
            final String currentFlavor
    ) throws JsonParserException {

        final JsonObject newpipeVersionInfo = JsonParser.object()
                .from(jsonData).getObject("flavors")
                .getObject("github").getObject("stable");

        final Optional<Object> flavorAlternative =
                newpipeVersionInfo.getArray("alternative_apks").stream().filter(obj -> {
                    final JsonObject jsonObj = (JsonObject) obj;
                    return jsonObj.getString("alternative").equals(currentFlavor);
                }).findFirst();


        if (flavorAlternative.isPresent()) {
            final JsonObject jsonObj = (JsonObject) flavorAlternative.get();
            newpipeVersionInfo.put("apk", jsonObj.get("url"));
        }
        return newpipeVersionInfo;
    }
}
