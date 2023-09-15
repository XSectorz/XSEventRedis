package net.xsapi.panat.xseventredis.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.xsapi.panat.xseventredis.api.RedisDataObject;
import net.xsapi.panat.xseventredis.api.RedisPlayerData;
import net.xsapi.panat.xseventredis.api.XSScore;
import net.xsapi.panat.xseventredis.api.RedisDataObjectTypeAdapter;
import net.xsapi.panat.xseventredis.configuration.config;
import net.xsapi.panat.xseventredis.configuration.configLoader;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public final class core extends JavaPlugin {

    public static HashMap<String,HashMap<String,XSScore>> scoreRedis = new HashMap<>();
    public static HashMap<String,RedisPlayerData> tempObject = new HashMap<>();
    public static core plugin;

    @Override
    public void onEnable() {

        plugin = this;
        new configLoader();

        if(redisConnection()) {
            for(String server : config.customConfig.getStringList("cross-server.servers")) {
                subscribeToChannelAsync(config.customConfig.getString("redis.host-server") + "/" + server);
            }
            subscribeToChannelAsync("XSEventLogout/Channel/" + config.customConfig.getString("redis.host-server"));
            subscribeToChannelAsync("XSEventLogin/Channel/" + config.customConfig.getString("redis.host-server"));
            subscribeToChannelAsync("XSEventReset/" + config.customConfig.getString("redis.host-server"));
            Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
                @Override
                public void run() {
                    //sendMessageToRedisAsync("Sync/01","test sent to Sync/01");
                    //sendMessageToRedisAsync("Sync/02","test sent to Sync/02");
                    //Bukkit.getConsoleSender().sendMessage("CONVERT: {\"score\":{},\"key\":\"xsevent_siam_mob_killing\"}");
                    //convertToObject("{\"score\":{},\"key\":\"xsevent_siam_mob_killing\"}");
                    //Bukkit.getConsoleSender().sendMessage("--------------------");
                   // checkData();
                    sendDataRedisBack();
                }
            }, 0L, 200L);
        }

    }

    public static core getPlugin() {
        return plugin;
    }

    private void convertToObject(String json) {
        if (json.isEmpty()) {
            return;
        }

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(RedisDataObject.class, new RedisDataObjectTypeAdapter());
        Gson gson = gsonBuilder.create();

        RedisDataObject redisDataObject = gson.fromJson(json, RedisDataObject.class);

        String key = redisDataObject.key;
        HashMap<String, XSScore> scoreMap = redisDataObject.score;
     //  System.out.println("Key: " + key);
     //   for (String playerName : scoreMap.keySet()) {
     //       XSScore xsScore = scoreMap.get(playerName);
     //       System.out.println("Player: " + xsScore.getPlayerName());
        //      System.out.println("Score: " + xsScore.getScore());
       // }

        //key == ชื่อIDEvent
        if(scoreRedis.containsKey(key)) {
            HashMap<String,XSScore> tempMap = scoreRedis.get(key);
            for(Map.Entry<String,XSScore> map : scoreMap.entrySet()) {
                if(tempMap.containsKey(map.getKey())) {
                    if(tempMap.get(map.getKey()).getScore() < map.getValue().getScore()) {
                        //Bukkit.getConsoleSender().sendMessage("Contain (but in temp data less than new data) REPLACE!");
                        tempMap.put(map.getKey(),map.getValue());
                    } else {
                        //Bukkit.getConsoleSender().sendMessage("Contain (but in temp data more than new data) SKIP!");
                    }
                } else {
                    tempMap.put(map.getKey(),map.getValue());
                }
            }
        } else {
            scoreRedis.put(key,scoreMap);
        }
    }

    public static void checkData() {
        for(Map.Entry<String,HashMap<String,XSScore>> event : scoreRedis.entrySet()) {
           // Bukkit.getConsoleSender().sendMessage("Event : " + event.getKey());
            for(Map.Entry<String,XSScore> score : event.getValue().entrySet()) {
                Bukkit.broadcastMessage("Player : " + score.getValue().getPlayerName() + ", " + score.getValue().getScore());
            }
        }
    }

    private void sendDataRedisBack() {
        Gson gson = new Gson();
        String jsonString = gson.toJson(scoreRedis);
        sendMessageToRedisAsync("XSEventRedisData/"+ config.customConfig.getString("redis.host-server"),jsonString);
       // Bukkit.broadcastMessage("Send.... From " + "XSEventRedisData/"+ config.customConfig.getString("redis.host-server"));
    }

    private void subscribeToChannelAsync(String channelName) {
        String redisHost = config.customConfig.getString("redis.host");
        int redisPort = config.customConfig.getInt("redis.port");
        String password = config.customConfig.getString("redis.password");

        new Thread(() -> {
            try (Jedis jedis = new Jedis(redisHost, redisPort)) {
                if(!password.isEmpty()) {
                    jedis.auth(password);
                }
                JedisPubSub jedisPubSub = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                      //  Bukkit.getConsoleSender().sendMessage("Received data from ---> " + channel);

                        if(channel.equalsIgnoreCase("XSEventLogout/Channel/" + config.customConfig.getString("redis.host-server"))) {

                        //    Bukkit.getConsoleSender().sendMessage("Get Player Data: " + message);
                            Gson gson = new Gson();
                            RedisPlayerData playerData = gson.fromJson(message, RedisPlayerData.class);

                        //    Bukkit.getConsoleSender().sendMessage("UUID : " + playerData.getUuid() );
                        //    Bukkit.getConsoleSender().sendMessage("NAME : " + playerData.getName() );
                        //    Bukkit.getConsoleSender().sendMessage("SCORE : " + playerData.getScoreList() );

                            tempObject.put(playerData.getUuid(),playerData);
                        } else if(channel.equalsIgnoreCase("XSEventLogin/Channel/" + config.customConfig.getString("redis.host-server"))) {
                         //   Bukkit.getConsoleSender().sendMessage("Recieved XSEventLogin Request ---> " + message);
                            String uuid = message.split(":")[1];
                            String server = message.split(":")[0];
                           // Bukkit.getConsoleSender().sendMessage("UUID: " + uuid);
                           // Bukkit.getConsoleSender().sendMessage("SERVER: " + server);
                            if(tempObject.containsKey(uuid)) {
                            //    Bukkit.getConsoleSender().sendMessage("Contain Send Data....");
                                Gson gson = new Gson();
                                String jsonString = gson.toJson(tempObject.get(uuid));
                                sendMessageToRedisAsync("LoginEvent/"+server,jsonString);
                                tempObject.remove(uuid);
                            }
                        } else if(channel.equalsIgnoreCase("XSEventReset/" + config.customConfig.getString("redis.host-server"))) {
                         //   Bukkit.getConsoleSender().sendMessage("Recieved XSEventReset/SyncHost/01 Request ---> " + message);
                            if(scoreRedis.containsKey(message)) {
                                if(!scoreRedis.get(message).isEmpty()) {
                                    scoreRedis.remove(message);
                                  //  Bukkit.getConsoleSender().sendMessage("RESET!");
                                }
                            }
                        } else {
                            for(String server : config.customConfig.getStringList("cross-server.servers")) {
                                if(channel.equalsIgnoreCase(config.customConfig.getString("redis.host-server") + "/" + server)) {
                                    convertToObject(message);
                              //      Bukkit.getConsoleSender().sendMessage("Recieved Player Data From ---> " + channel);
                                }
                            }
                            //Bukkit.getConsoleSender().sendMessage("Recieved Player Data From ---> " + channel);
                           // convertToObject(message);
                        }

                       // Bukkit.getConsoleSender().sendMessage(message);
                       // Bukkit.getConsoleSender().sendMessage("-------------------------------");
                    }
                };
                jedis.subscribe(jedisPubSub, channelName);
            } catch (Exception e) {
                // จัดการข้อผิดพลาดที่เกิดขึ้น
                e.printStackTrace();
            }
        }).start();
    }

    private void sendMessageToRedisAsync(String CHName, String message) {
        String redisHost = config.customConfig.getString("redis.host");
        int redisPort = config.customConfig.getInt("redis.port");
        String password = config.customConfig.getString("redis.password");

        new Thread(() -> {
            try (Jedis jedis = new Jedis(redisHost, redisPort)) {
                if(!password.isEmpty()) {
                    jedis.auth(password);
                }
                jedis.publish(CHName, message);
            } catch (Exception e) {
                // จัดการข้อผิดพลาดที่เกิดขึ้น
                e.printStackTrace();
            }
        }).start();
    }

    private boolean redisConnection() {
        String redisHost = config.customConfig.getString("redis.host");
        int redisPort = config.customConfig.getInt("redis.port");
        String password = config.customConfig.getString("redis.password");

        try {
            Jedis jedis = new Jedis(redisHost, redisPort);
            if(!password.isEmpty()) {
                jedis.auth(password);
            }
            jedis.close();
            Bukkit.getConsoleSender().sendMessage("§x§E§7§F§F§0§0[XSEVENT REDIS] Redis Server : §x§6§0§F§F§0§0Connected");
            return true;
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage("§x§E§7§F§F§0§0[XSEVENT REDIS] Redis Server : §x§C§3§0§C§2§ANot Connected");
            e.printStackTrace();
        }
        return false;
    }
}