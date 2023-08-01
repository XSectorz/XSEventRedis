package net.xsapi.panat.xseventredis.api;

import java.util.HashMap;

public class RedisDataObject {
    public HashMap<String, XSScore> score;
    public String key;

    public RedisDataObject(String key,HashMap<String ,XSScore> score) {
        this.key = key;
        this.score = score;
    }
}
