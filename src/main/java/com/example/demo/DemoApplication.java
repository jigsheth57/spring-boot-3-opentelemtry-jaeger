package com.example.demo;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;

@SpringBootApplication
@Configuration
public class DemoApplication {

  @Value("${spring.application.name}")
  private String applicationName;

  public static void main(String[] args) {
    SpringApplication.run(DemoApplication.class, args);
  }

  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder.build();
  }

  // Init OpenTelemetry
  @Bean
  public OpenTelemetry initOpenTelemetry(@Value("${otel.exporter.otlp.endpoint}") String jaegerEndpoint) {

    // Include required service.name resource attribute on all spans and metrics
    // autowire's not working in boot v3.0.x or v3.1.x
    Resource resource = Resource.getDefault()
        .merge(Resource.builder().put("service.name", applicationName).build());

    // Export traces to Jaeger over OTLP
    OtlpGrpcSpanExporter jaegerOtlpExporter = OtlpGrpcSpanExporter.builder()
        .setEndpoint(jaegerEndpoint)
        .setTimeout(30, TimeUnit.SECONDS)
        .build();

    // Set to process the spans by the Jaeger Exporter
    SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(BatchSpanProcessor.builder(jaegerOtlpExporter).build())
        .setResource(resource)
        .build();
    OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();

    // it's always a good idea to shut down the SDK cleanly at JVM exit.
    Runtime.getRuntime().addShutdownHook(new Thread(tracerProvider::close));

    return openTelemetry;
  }
  // @Bean
  // public OtlpGrpcSpanExporter otlpHttpSpanExporter(@Value("${otel.exporter.otlp.endpoint}")
  // String url) {
  // return OtlpGrpcSpanExporter.builder().setEndpoint(url).build();
  // }
}
