package com.example.demo;


import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RedisExpirationListenerServices {

    private final RedisClusterClient redisClusterClient;

    //    @Value("${spring.redis.cluster.nodes}")
    private final String redisNodes = "192.168.37.88:6350,192.168.37.88:6351,192.168.37.88:6352"; // Comma-separated list of Redis cluster nodes

    public RedisExpirationListenerServices() {
        // Initialize Redis Cluster Client
        List<RedisURI> clusterNodes = Arrays.stream(redisNodes.split(","))
                .map(node -> {
                    String[] parts = node.split(":");
                    return RedisURI.create(parts[0], Integer.parseInt(parts[1]));
                })
                .collect(Collectors.toList());

        this.redisClusterClient = RedisClusterClient.create(clusterNodes);
    }

    @PostConstruct
    public void startListening() {
        // Get all cluster nodes and construct RedisURI correctly
        List<RedisURI> clusterNodes = redisClusterClient.getPartitions().stream()
                .map(partition -> {
                    RedisURI uri = partition.getUri();  // Get RedisURI from partition
                    return RedisURI.builder()
                            .withHost(uri.getHost())  // Extract host from URI
                            .withPort(uri.getPort())  // Extract port from URI
                            .build();
                })
                .collect(Collectors.toList());

        System.out.println("Subscribing to Redis Cluster nodes: " + clusterNodes);

        // Enable keyspace notifications on all nodes
        clusterNodes.forEach(this::enableKeyspaceNotifications);

        // Subscribe to key expiry events on each node
        clusterNodes.forEach(this::subscribeToNode);
    }


    private void enableKeyspaceNotifications(RedisURI node) {
        try (RedisClient client = RedisClient.create(node.toString());
             var connection = client.connect()) {
            RedisClusterCommands<String, String> syncCommands = connection.sync();
            syncCommands.configSet("notify-keyspace-events", "Ex");
            System.out.println("Enabled key expiry events on: " + node);
        } catch (Exception e) {
            System.err.println("Failed to enable notifications on " + node + ": " + e.getMessage());
        }
    }

    private void subscribeToNode(RedisURI node) {
        new Thread(() -> {
            try (RedisClient client = RedisClient.create(node.toString());
                 StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub()) {

                connection.addListener(new RedisPubSubListener<>() {
                    @Override
                    public void message(String channel, String message) {
                        System.out.println("Subscribed to: " + channel + " on node " + node.getHost() + ":" + node.getPort()+"_________");
                        System.out.print("Key expired: " + message);
                        triggerApiCall(message);
                    }

                    @Override
                    public void message(String pattern, String channel, String message) {
                    }

                    @Override
                    public void subscribed(String channel, long count) {
                        System.out.println("Subscribed to: " + channel + " on " + node);
                    }

                    @Override
                    public void psubscribed(String pattern, long count) {
                    }

                    @Override
                    public void unsubscribed(String channel, long count) {
                    }

                    @Override
                    public void punsubscribed(String pattern, long count) {
                    }
                });

                connection.sync().subscribe("__keyevent@0__:expired");

                // Keep thread alive to listen for events
                while (true) {
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                System.err.println("Error subscribing to node " + node + ": " + e.getMessage());
            }
        }).start();
    }

    private void triggerApiCall(String key) {
        List<String> outputList = new ArrayList<>(Arrays.asList(key.split("_")));
        System.out.println("API call triggered for key: " + outputList);
    }
}
