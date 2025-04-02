# Flink con OpenTelemetry y Grafana Tempo

## Descripción

Este proyecto implementa una aplicación en **Flink** que consume datos de Kafka, aplica procesamiento de eventos y utiliza **OpenTelemetry** para la trazabilidad de los datos. Las trazas se exportan mediante el **OpenTelemetry Collector** y se visualizan en **Grafana Tempo**.

## Arquitectura

El proyecto se compone de los siguientes elementos:

- **Aplicación Flink**: Procesa eventos de Kafka y utiliza OpenTelemetry para trazabilidad.
- **Kafka**: Broker de mensajería utilizado para consumir y producir eventos.
- **OpenTelemetry Collector**: Recolector de trazas que actúa como hub central de procesamiento y transformación de trazas.
- **OpenTelemetry Exporter**: Envía las trazas al backend o plataforma de observabilidad de trazas.
- **Grafana Tempo**: Almacena y visualiza las trazas en Grafana.

## Prerrequisitos

Antes de ejecutar el proyecto, asegúrate de tener instalados:

- **Docker y Docker Compose**
- **Java 11+**
- **Kafka levantado en localhost:9092**

## Instalación y Ejecución

### 1. Levantar el entorno de OpenTelemetry y Grafana Tempo

Ejecuta el siguiente comando para iniciar los contenedores:

```sh
docker-compose up -d
```

Esto iniciará los servicios:

- **otel-collector** en `0.0.0.0:4317` (gRPC) y `0.0.0.0:4318` (HTTP)
- **tempo** en `0.0.0.0:3200 (o tempo:3200)` (para consultas de Grafana)
- **grafana** en `localhost:3000` (usuario: `admin`, contraseña: `admin`)

### 2. Construcción del Proyecto

```sh
mvn clean package
```

Esto generará el JAR ejecutable en la carpeta `target/`.

### 3. Iniciar el Productor de Datos de Sensores

Ejecuta el siguiente comando para enviar datos de ejemplo a Kafka:

```sh
java -Dotel.traces.exporter=otlp -Dotel.service.name=kafka-flink-opentelemetry-app -Dotel.log.level=debug -Dotel.java.global-autoconfigure.enabled=true      -cp target/kafka-tracing-with-opentelemetry-and-grafana-tempo-1.0-SNAPSHOT.jar bigdata.zylk.net.tracing.source.KafkaSensorDataProducer
```
También se puede ejecutar desde el propio IDE (p. ej. IntelliJ IDEA) con un RUN en el que en el se debería especificar en la configuración del RUN los argumentos de VM anteriores:

```
-Dotel.traces.exporter=otlp
-Dotel.service.name=kafka-flink-opentelemetry-app
-Dotel.log.level=debug 
-Dotel.java.global-autoconfigure.enabled=true
```


### 4. Ejecutar la Aplicación de Flink

Para ejecutar la aplicación de Flink que lea los mensajes del productor de Kafka anterior, ejecutamos el siguiente comando desde el directorio root de nuestro proyecto, o hacemos un RUN desde el IDE con los comandos de VM especificados anteriormente.

```sh
java -Dotel.traces.exporter=otlp -Dotel.service.name=kafka-flink-opentelemetry-app -Dotel.log.level=debug -Dotel.java.global-autoconfigure.enabled=true  -cp target/kafka-tracing-with-opentelemetry-and-grafana-tempo-1.0-SNAPSHOT.jar bigdata.zylk.net.tracing.flink.FlinkKafkaTracingApp
```

### 5. Visualizar las Trazas en Grafana

1. Abre Grafana en `http://localhost:3000`
2. Ve a **Configuration > Data Sources** y añade `Tempo` como fuente de datos con `http://tempo:3200`.
3. Dirígete a **Explore** y selecciona `Tempo` para visualizar las trazas.

## Configuración

### OpenTelemetry Collector (`otel-collector.yml`)

```yaml
receivers:
  otlp:
    protocols:
      http:
        endpoint: 0.0.0.0:4318
      grpc:
        endpoint: 0.0.0.0:4317
processors:
  batch:
exporters:
  otlp/tempo:
    endpoint: "tempo:4317"
    tls:
      insecure: true
service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [otlp/tempo]
```

### Configuración de Kafka con OpenTelemetry

La aplicación de Flink y el productor de Kafka utilizan **interceptores de OpenTelemetry**:

#### Productor (`KafkaSensorDataProducer`)

```java
props.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, TracingProducerInterceptor.class.getName());
```

#### Consumidor en Flink (`FlinkKafkaTracingAppWithPOJO`)

```java
props.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, TracingConsumerInterceptor.class.getName());
```

## Referencias

- [Apache Flink](https://flink.apache.org/)
- [Apache Kafka](https://kafka.apache.org/)
- [OpenTelemetry](https://opentelemetry.io/)
- [Grafana Tempo](https://grafana.com/oss/tempo/)