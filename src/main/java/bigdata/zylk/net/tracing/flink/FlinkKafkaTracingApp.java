package bigdata.zylk.net.tracing.flink;

import io.opentelemetry.instrumentation.kafkaclients.v2_6.TracingConsumerInterceptor;
import io.opentelemetry.instrumentation.kafkaclients.v2_6.TracingProducerInterceptor;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.CoFlatMapFunction;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import org.apache.kafka.clients.producer.ProducerConfig;

import java.util.Properties;

public class FlinkKafkaTracingApp {
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String GROUP_ID = "flink-group";
    private static final String OUTPUT_TOPIC = "output-topic";

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        KafkaSource<String> pressureSource = createKafkaSource("pressure-topic");
        KafkaSource<String> temperatureSource = createKafkaSource("temperature-topic");

        DataStream<String> pressureStream = env.fromSource(
                pressureSource,
                WatermarkStrategy.noWatermarks(),
                "Pressure Source"
        );

        DataStream<String> temperatureStream = env.fromSource(
                temperatureSource,
                WatermarkStrategy.noWatermarks(),
                "Temperature Source"
        );

        DataStream<String> processedStream = pressureStream
                .connect(temperatureStream)
                .flatMap(new CombinePressureAndTemperature());

        processedStream.sinkTo(createKafkaSink());

        env.execute("Flink Kafka Tracing App");
    }

    private static KafkaSource<String> createKafkaSource(String topic) {
        Properties consumerProps = getConsumerProperties();

        return KafkaSource.<String>builder()
                .setBootstrapServers(BOOTSTRAP_SERVERS)
                .setTopics(topic)
                .setGroupId(GROUP_ID)
                .setProperties(consumerProps)
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();
    }

    private static KafkaSink<String> createKafkaSink() {
        Properties producerProps = getProducerProperties();

        return KafkaSink.<String>builder()
                .setBootstrapServers(BOOTSTRAP_SERVERS)
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic(OUTPUT_TOPIC)
                        .setValueSerializationSchema(new SimpleStringSchema())
                        .build())
                .setKafkaProducerConfig(producerProps)
                .build();
    }

    private static Properties getConsumerProperties() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, TracingConsumerInterceptor.class.getName());

        return props;
    }

    private static Properties getProducerProperties() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, TracingProducerInterceptor.class.getName());

        return props;
    }

    public static class CombinePressureAndTemperature
            implements CoFlatMapFunction<String, String, String> {

        @Override
        public void flatMap1(String pressure, Collector<String> out) {
            out.collect(String.format("PRESSURE|%s|%d", pressure, System.currentTimeMillis()));
        }

        @Override
        public void flatMap2(String temperature, Collector<String> out) {
            out.collect(String.format("TEMPERATURE|%s|%d", temperature, System.currentTimeMillis()));
        }
    }
}
