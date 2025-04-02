package bigdata.zylk.net.tracing.flink;

import bigdata.zylk.net.tracing.pojo.SensorRecord;
import bigdata.zylk.net.SensorRecordSerializationSchema;
import io.opentelemetry.instrumentation.kafkaclients.v2_6.TracingConsumerInterceptor;
import io.opentelemetry.instrumentation.kafkaclients.v2_6.TracingProducerInterceptor;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.CoFlatMapFunction;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.util.Properties;

public class FlinkKafkaTracingAppWithPOJO {
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

        DataStream<SensorRecord> processedStream = pressureStream
                .connect(temperatureStream)
                .flatMap(new CombinePressureAndTemperature());

        KafkaSink<SensorRecord> kafkaSink = KafkaSink.<SensorRecord>builder()
                .setBootstrapServers(BOOTSTRAP_SERVERS)
                .setRecordSerializer(new SensorRecordSerializationSchema())
                .setKafkaProducerConfig(getProducerProperties())
                .build();

        processedStream.sinkTo(kafkaSink);

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

    private static Properties getConsumerProperties() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG,
                TracingConsumerInterceptor.class.getName());
        return props;
    }

    private static Properties getProducerProperties() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG,
                TracingProducerInterceptor.class.getName());
        return props;
    }

    public static class CombinePressureAndTemperature
            implements CoFlatMapFunction<String, String, SensorRecord> {

        @Override
        public void flatMap1(String pressure, Collector<SensorRecord> out) {
            out.collect(new SensorRecord(
                    "pressure-key",
                    "PRESSURE|" + pressure + "|" + System.currentTimeMillis()
            ));
        }

        @Override
        public void flatMap2(String temperature, Collector<SensorRecord> out) {
            out.collect(new SensorRecord(
                    "temperature-key",
                    "TEMPERATURE|" + temperature + "|" + System.currentTimeMillis()
            ));
        }
    }
}
