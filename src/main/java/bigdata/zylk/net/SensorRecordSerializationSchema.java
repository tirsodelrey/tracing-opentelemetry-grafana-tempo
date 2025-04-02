package bigdata.zylk.net;

import bigdata.zylk.net.tracing.pojo.SensorRecord;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.nio.charset.StandardCharsets;

public class SensorRecordSerializationSchema  implements KafkaRecordSerializationSchema<SensorRecord> {
    private final SimpleStringSchema keySerializer = new SimpleStringSchema();
    private final SimpleStringSchema valueSerializer = new SimpleStringSchema();
    private static final String OUTPUT_TOPIC = "output-topic";
    @Override
    public ProducerRecord<byte[], byte[]> serialize(
            SensorRecord element,
            KafkaSinkContext context,
            Long timestamp
    ) {
        return new ProducerRecord<>(
                OUTPUT_TOPIC,
                //null, // partition
                //element.getKey().getBytes(StandardCharsets.UTF_8),
                element.getValue().getBytes(StandardCharsets.UTF_8)
        );
    }
}
