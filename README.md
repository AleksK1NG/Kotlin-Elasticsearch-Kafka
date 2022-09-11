Kotlin Spring, ElasticSearch and Kafka full-text search microservice 👋✨💫

#### 👨‍💻 Full list what has been used:
[Spring](https://spring.io/) Spring web framework <br/>
[Spring WebFlux](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html) Reactive REST Services <br/>
[Elasticsearch](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/introduction.html) client for Java <br/>
[Kafka](https://docs.spring.io/spring-kafka/reference/html/#preface) Spring Kafka <br/>
[Zipkin](https://zipkin.io/) open source, end-to-end distributed [tracing](https://opentracing.io/) <br/>
[Spring Cloud Sleuth](https://docs.spring.io/spring-cloud-sleuth/docs/current-SNAPSHOT/reference/html/index.html) auto-configuration for distributed tracing <br/>
[Prometheus](https://prometheus.io/) monitoring and alerting <br/>
[Grafana](https://grafana.com/) for to compose observability dashboards with everything from Prometheus <br/>
[Kibana](https://www.elastic.co/kibana/) is user interface that lets you visualize your Elasticsearch <br/>
[Docker](https://www.docker.com/) and docker-compose <br/>

All UI interfaces will be available on ports:

#### Swagger UI: http://localhost:8000/webjars/swagger-ui
<img src="https://i.postimg.cc/3RHKskKK/Swagger-UI-2022-09-11-10-48-29.png" alt="Swagger"/>

#### Grafana UI: http://localhost:3005
<img src="https://i.postimg.cc/c1FSm981/Spring-Boot-Statistics-Endpoint-Metrics-Grafana-2022-09-11-11-09-57.png" alt="Grafana"/>

#### Kibana UI: http://localhost:5601/app/home#/
<img src="https://i.postimg.cc/gk84KXGH/Console-Dev-Tools-Elastic-2022-08-14-14-59-58.png" alt="Kibana"/>

#### Zipkin UI: http://localhost:16686
<img src="https://i.postimg.cc/Y2xmrHLr/Zipkin-2022-09-11-13-43-34.png" alt="Zipkin"/>

#### Prometheus UI: http://localhost:9090
<img src="https://i.postimg.cc/DZfN4h6r/Prometheus-Time-Series-Collection-and-Processing-Server-2022-09-11-12-03-44.pngg" alt="Prometheus"/>


For local development 🙌👨‍💻🚀:

```
make local // for run docker compose
```
or
```
make develop // run all in docker compose with hot reload
```