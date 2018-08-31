package com.bitnei.main;

import kafka.utils.ZKGroupTopicDirs;
import kafka.utils.ZkUtils;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.security.JaasUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scala.Option;
import scala.collection.JavaConverters;

import java.util.*;

/**
 * Created on 2018/8/30.
 *
 * @author zhaogd
 */
public class MigrateKafkaOffsets {
    private static final Logger logger = LogManager.getLogger(MigrateKafkaOffsets.class);

    private static final int ZK_SESSION_TIMEOUT = 30000;
    private static final int ZK_CONNECTION_TIMEOUT = 30000;
    private static ZkUtils zkUtils;
    private static Properties kafkaProps;

    public static void main(String[] args) throws Exception {
        logger.info("args=>{}", Arrays.toString(args));
        try {
            initConfig(args);
            final List<String> groupList = JavaConverters.seqAsJavaListConverter(zkUtils.getConsumerGroups()).asJava();

            for (String group : groupList) {
                logger.info("Group found: {}", group);
                final List<String> topics = JavaConverters.seqAsJavaListConverter(zkUtils.getTopicsByConsumerGroup(group)).asJava();
                logger.info("Topics found: {}", topics);
                if (topics.isEmpty()) {
                    logger.info("Topics is empty, will not migrate.");
                    continue;
                }
                for (String topic : topics) {
                    migrateOffsets(group, topic);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            zkUtils.close();
        }
    }

    private static void initConfig(String[] args) {
        logger.info("Init config begin...");
        String zookeeperConnect = args[0];
        zkUtils = ZkUtils.apply(zookeeperConnect, ZK_SESSION_TIMEOUT, ZK_CONNECTION_TIMEOUT, JaasUtils.isZkSecurityEnabled());

        kafkaProps = new Properties();
        kafkaProps.put("bootstrap.servers", args[1]);
        kafkaProps.put("enable.auto.commit", "false");
        kafkaProps.put("session.timeout.ms", "30000");
        kafkaProps.put("max.poll.records", "100");
        kafkaProps.put("auto.commit.interval.ms", "1000");
        kafkaProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        logger.info("Init config end...");
    }

    private static void migrateOffsets(String groupId, String topicStr) throws Exception {
        logger.info("Begin migrate, group={},topic={}", groupId, topicStr);

        kafkaProps.put("group.id", groupId);
        try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(kafkaProps)) {
            Map<TopicPartition, OffsetAndMetadata> kafkaOffsets =
                    getKafkaOffsets(consumer, topicStr);
            if (!kafkaOffsets.isEmpty()) {
                logger.info("Found Kafka offsets for topic " + topicStr +
                        ". Will not migrate from zookeeper");
                logger.debug("Offsets found: {}", kafkaOffsets);
                return;
            }

            logger.info("No Kafka offsets found. Migrating zookeeper offsets");
            Map<TopicPartition, OffsetAndMetadata> zookeeperOffsets =
                    getZookeeperOffsets(zkUtils, groupId, topicStr);
            if (zookeeperOffsets.isEmpty()) {
                logger.warn("No offsets to migrate found in Zookeeper");
                return;
            }

            logger.info("Committing Zookeeper offsets to Kafka");
            logger.debug("Offsets to commit: {}", zookeeperOffsets);
            consumer.commitSync(zookeeperOffsets);
            // Read the offsets to verify they were committed
            Map<TopicPartition, OffsetAndMetadata> newKafkaOffsets =
                    getKafkaOffsets(consumer, topicStr);
            logger.debug("Offsets committed: {}", newKafkaOffsets);
            if (!newKafkaOffsets.keySet().containsAll(zookeeperOffsets.keySet())) {
                throw new Exception("Offsets could not be committed");
            }
        }
    }

    private static Map<TopicPartition, OffsetAndMetadata> getKafkaOffsets(
            KafkaConsumer<String, byte[]> client, String topicStr) {
        Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
        List<PartitionInfo> partitions = client.partitionsFor(topicStr);
        for (PartitionInfo partition : partitions) {
            TopicPartition key = new TopicPartition(topicStr, partition.partition());
            OffsetAndMetadata offsetAndMetadata = client.committed(key);
            if (offsetAndMetadata != null) {
                offsets.put(key, offsetAndMetadata);
            }
        }
        return offsets;
    }

    private static Map<TopicPartition, OffsetAndMetadata> getZookeeperOffsets(ZkUtils client, String groupId, String topicStr) {
        Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
        ZKGroupTopicDirs topicDirs = new ZKGroupTopicDirs(groupId, topicStr);
        List<String> partitions = JavaConverters.seqAsJavaListConverter(
                client.getChildrenParentMayNotExist(topicDirs.consumerOffsetDir())).asJava();
        for (String partition : partitions) {
            TopicPartition key = new TopicPartition(topicStr, Integer.valueOf(partition));
            Option<String> data = client.readDataMaybeNull(
                    topicDirs.consumerOffsetDir() + "/" + partition)._1();
            if (data.isDefined()) {
                long offset = Long.parseLong(data.get());
                offsets.put(key, new OffsetAndMetadata(offset));
            }
        }
        return offsets;
    }
}
