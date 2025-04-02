package bigdata.zylk.net.tracing.source;
import io.opentelemetry.instrumentation.kafkaclients.v2_6.TracingProducerInterceptor;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class KafkaSensorDataProducer {
    private static volatile boolean running = true;
    private static Thread producerThread;
    private static Producer<String, String> producer;

    public static void start() {
        final AtomicBoolean initialised = new AtomicBoolean(false);

        producerThread = new Thread(() -> {
            try {
                producer = createProducer();
                initialised.set(true);
                produceData();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                if (producer != null) {
                    producer.close();
                }
            }
        });

        producerThread.start();

        // Esperar a que el productor se inicialice
        while (!initialised.get()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Añadir hook de shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stop();
            System.out.println("Producer shutdown complete");
        }));
    }

    public static void stop() {
        running = false;
        if (producerThread != null && producerThread.isAlive()) {
            producerThread.interrupt();
            try {
                producerThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void produceData() throws InterruptedException {
        Random random = new Random();

        while (running) {
            int pressureValue = 900 + random.nextInt(300);
            int temperatureValue = 20 + random.nextInt(15);

            producer.send(new ProducerRecord<>("pressure-topic", "pressure",
                    String.valueOf(pressureValue)));
            producer.send(new ProducerRecord<>("temperature-topic", "temperature",
                    String.valueOf(temperatureValue)));

            System.out.printf("Sent Pressure: %d hPa | Sent Temperature: %d °C%n",
                    pressureValue, temperatureValue);

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                if (!running) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private static Producer<String, String> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, TracingProducerInterceptor.class.getName());
        return new KafkaProducer<>(props);
    }

    // Para usar desde el main
    public static void main(String[] args) throws Exception {
        KafkaSensorDataProducer.start();

        // Ejemplo: parar después de 60 segundos
        Thread.sleep(10000);
        KafkaSensorDataProducer.stop();
    }
}
