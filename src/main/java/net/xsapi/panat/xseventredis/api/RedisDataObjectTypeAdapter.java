package net.xsapi.panat.xseventredis.api;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RedisDataObjectTypeAdapter extends TypeAdapter<RedisDataObject> {

    @Override
    public void write(JsonWriter out, RedisDataObject redisDataObject) throws IOException {
        if (redisDataObject == null) {
            out.nullValue();
        } else {
            out.beginObject();
            out.name("key");
            out.value(redisDataObject.key);
            out.name("score");
            out.beginObject();
            for (Map.Entry<String, XSScore> entry : redisDataObject.score.entrySet()) {
                out.name(entry.getKey());
                writeXSScore(out, entry.getValue());
            }
            out.endObject();
            out.endObject();
        }
    }

    @Override
    public RedisDataObject read(JsonReader in) throws IOException {
        in.beginObject();
        String key = null;
        HashMap<String, XSScore> score = new HashMap<>();

        while (in.hasNext()) {
            String name = in.nextName();
            if (name.equals("key")) {
                key = in.nextString();
            } else if (name.equals("score")) {
                in.beginObject();
                while (in.hasNext()) {
                    String playerName = in.nextName();
                    XSScore xsScore = readXSScore(in);
                    score.put(playerName, xsScore);
                }
                in.endObject();
            } else {
                in.skipValue();
            }
        }

        in.endObject();

        if (key == null) {
            throw new IOException("Missing 'key' field in JSON");
        }

        return new RedisDataObject(key, score);
    }

    private void writeXSScore(JsonWriter out, XSScore xsScore) throws IOException {
        if (xsScore == null) {
            out.nullValue();
        } else {
            out.beginObject();
            out.name("player");
            out.value(xsScore.getPlayerName());
            out.name("score");
            out.value(xsScore.getScore());
            out.endObject();
        }
    }

    private XSScore readXSScore(JsonReader in) throws IOException {
        in.beginObject();
        String playerName = null;
        double score = 0;

        while (in.hasNext()) {
            String name = in.nextName();
            if (name.equals("player")) {
                playerName = in.nextString();
            } else if (name.equals("score")) {
                score = in.nextDouble();
            } else {
                in.skipValue();
            }
        }

        in.endObject();

        if (playerName == null) {
            throw new IOException("Missing 'player' field in JSON");
        }

        XSScore xsScore = new XSScore(playerName);
        xsScore.setScore(score);

        return xsScore;
    }
}

