//package com.example.demo;
//
//import io.lettuce.core.RedisClient;
//import io.lettuce.core.api.sync.RedisCommands;
//import io.lettuce.core.pubsub.RedisPubSubListener;
//import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
//import jakarta.annotation.PostConstruct;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//
//@Service
//public class RedisExpirationListenerService {
//
//    private final RedisClient redisClient;
//
//    @Value("${spring.redis.host}")
//    private String redisHost;
//
//    public RedisExpirationListenerService() {
//        // Initialize Redis client with proper host
//        this.redisClient = RedisClient.create("redis://localhost:6379");
//    }
//
//    @PostConstruct
//    public void startListening() {
//        // Create a Pub/Sub connection to handle subscriptions
//        StatefulRedisPubSubConnection<String, String> connection = redisClient.connectPubSub();
//
//        // Enable keyspace notifications for expired events
//        RedisCommands<String, String> syncCommands = redisClient.connect().sync();
//        String response = syncCommands.configSet("notify-keyspace-events", "Ex");
//        System.out.println("Config response: " + response);
//
//        // Add listener to handle messages
//        connection.addListener(new RedisPubSubListener<String, String>() {
//            @Override
//            public void message(String channel, String message) {
//                // This is the expired key
//                System.out.println("Key expired: " + message);
//
//                // Retrieve the value of the expired key
//                String value = syncCommands.get(message);
//
//                // Trigger the API call with the expired key and its value
//                triggerApiCall(message, value);
//            }
//
//            @Override
//            public void message(String pattern, String channel, String message) {
//                // Not used, since we don't use patterns
//            }
//
//            @Override
//            public void subscribed(String channel, long count) {
//                // Optional: log or handle the subscription
//                System.out.println("Subscribed to channel: " + channel);
//            }
//
//            @Override
//            public void psubscribed(String pattern, long count) {
//                // Not used
//            }
//
//            @Override
//            public void unsubscribed(String channel, long count) {
//                // Optional: log or handle the unsubscription
//            }
//
//            @Override
//            public void punsubscribed(String pattern, long count) {
//                // Not used
//            }
//        });
//
//        // Subscribe to the expiration channel
//        connection.sync().subscribe("__keyevent@0__:expired");
//
//        // Keep the application running to listen for events
//        System.out.println("Listening for key expiration events...");
//        while (true) {
//            try {
//                Thread.sleep(1000);  // Keep the thread alive
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }
//    }
//
//    private void triggerApiCall(String key, String value) {
//        // Implement your API call logic here
//        String output[] = key.split("_");
//        ArrayList<String> outputList = new ArrayList<>(Arrays.asList(output));
//
//
//        System.out.println("API call triggered for key: " + outputList);
//    }
//}
