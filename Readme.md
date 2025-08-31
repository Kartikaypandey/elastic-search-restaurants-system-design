# Business Search Demo (Spring Boot + Elasticsearch + Kibana)

A minimal, production-style sample where users can:

- **Search businesses** by keyword (name/description/categories) and optional **location radius**
- **View details** of a single business by ID

Stack: Spring Boot 3.x, Spring Data Elasticsearch, Java 21, Docker Compose (Elasticsearch + Kibana + App)

---

## Project structure
```
business-search/
├─ docker-compose.yml
├─ pom.xml
├─ README.md
├─ src/main/java/com/example/businesssearch/
│  ├─ BusinessSearchApplication.java
│  ├─ config/ElasticsearchConfig.java (optional)
│  ├─ model/Business.java
│  ├─ repo/BusinessRepository.java
│  ├─ service/BusinessService.java
│  ├─ web/BusinessController.java
│  └─ bootstrap/SampleDataLoader.java
└─ src/main/resources/
   └─ application.yml
```

---

## docker-compose.yml
```yaml
version: "3.9"

services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.14.3
    container_name: es8
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - ES_JAVA_OPTS=-Xms1g -Xmx1g
    ports:
      - "9200:9200"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9200"]
      interval: 10s
      timeout: 5s
      retries: 30

  kibana:
    image: docker.elastic.co/kibana/kibana:8.14.3
    container_name: kibana8
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
      - XPACK_SECURITY_ENABLED=false
    ports:
      - "5601:5601"
    depends_on:
      - elasticsearch

  app:
    build: .
    container_name: business-search-app
    environment:
      - SPRING_ELASTICSEARCH_URIS=http://elasticsearch:9200
      - JAVA_TOOL_OPTIONS=-Xms256m -Xmx512m
    ports:
      - "8080:8080"
    depends_on:
      elasticsearch:
        condition: service_healthy
```

> **Note:** Security is disabled for simplicity.

---

## pom.xml (Maven)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>com.example</groupId>
  <artifactId>business-search</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <name>business-search</name>
  <description>Business Search demo with Spring Boot & Elasticsearch</description>
  <properties>
    <java.version>21</java.version>
    <spring.boot.version>3.3.2</spring.boot.version>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-dependencies</artifactId>
        <version>${spring.boot.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <dependency>
      <groupId>org.springframework.data</groupId>
      <artifactId>spring-data-elasticsearch</artifactId>
    </dependency>

    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <optional>true</optional>
    </dependency>

    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
    </dependency>

    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

---

## application.yml
```yaml
server:
  port: 8080
spring:
  application:
    name: business-search
  elasticsearch:
    uris: ${SPRING_ELASTICSEARCH_URIS:http://localhost:9200}
  jackson:
    default-property-inclusion: non_null
```

---

## BusinessSearchApplication.java
```java
package com.example.businesssearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BusinessSearchApplication {
  public static void main(String[] args) {
    SpringApplication.run(BusinessSearchApplication.class, args);
  }
}
```

---

## model/Business.java
```java
package com.example.businesssearch.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.geo.Point;

import java.util.List;

@Document(indexName = "businesses")
@Setter @Getter @Builder
@AllArgsConstructor @NoArgsConstructor
public class Business {
  @Id
  private String id;

  @Field(type = FieldType.Text, analyzer = "standard")
  private String name;

  @Field(type = FieldType.Text, analyzer = "standard")
  private String description;

  @Field(type = FieldType.Keyword)
  private List<String> categories;

  @Field(type = FieldType.Text, analyzer = "standard")
  private String address;

  @GeoPointField
  private Point location; // longitude,latitude order in Spring Data Point

  @Field(type = FieldType.Keyword)
  private String phone;

  @Field(type = FieldType.Keyword)
  private String website;

  @Field(type = FieldType.Double)
  private Double rating; // 0.0 - 5.0
}
```

> **Geo note:** `Point` constructor is `(x=lon, y=lat)`. When sending JSON, we’ll accept `lat` and `lon` separately and convert.

---

## repo/BusinessRepository.java
```java
package com.example.businesssearch.repo;

import com.example.businesssearch.model.Business;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface BusinessRepository extends ElasticsearchRepository<Business, String> { }
```

---

## service/BusinessService.java
```java
package com.example.businesssearch.service;

import com.example.businesssearch.model.Business;
import com.example.businesssearch.repo.BusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.geo.GeoDistanceOrder;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BusinessService {
  private final BusinessRepository repository;
  private final ElasticsearchOperations operations;

  public Business save(Business b) { return repository.save(b); }
  public Business get(String id) { return repository.findById(id).orElse(null); }

  public Page<Business> search(String q, Double lat, Double lon, String radiusKm, int page, int size, boolean sortByDistance) {
    Criteria text = new Criteria();
    if (StringUtils.hasText(q)) {
      text = new Criteria("name").matches(q)
              .or(new Criteria("description").matches(q))
              .or(new Criteria("categories").matches(q));
    }

    Criteria geo = null;
    if (lat != null && lon != null && StringUtils.hasText(radiusKm)) {
      geo = new Criteria("location").within(new Point(lon, lat), new Distance(Double.parseDouble(radiusKm), Metrics.KILOMETERS));
    }

    Criteria combined = (geo != null && text != null && StringUtils.hasText(q)) ? text.and(geo)
                        : (geo != null ? geo : text);

    CriteriaQuery query = new CriteriaQuery(combined);
    query.setPageable(PageRequest.of(page, size));

    if (sortByDistance && lat != null && lon != null) {
      query.addSort(new GeoDistanceOrder("location", new Point(lon, lat)).withUnit(Metrics.KILOMETERS).with(Sort.Direction.ASC));
    } else {
      query.addSort(Sort.by(Sort.Direction.DESC, "_score"));
    }

    return operations.search(query, Business.class).map(hit -> hit.getContent());
  }
}
```

---

## web/BusinessController.java
```java
package com.example.businesssearch.web;

import com.example.businesssearch.model.Business;
import com.example.businesssearch.service.BusinessService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/businesses")
@RequiredArgsConstructor
@Validated
public class BusinessController {
  private final BusinessService service;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Business create(@RequestBody Map<String, Object> body) {
    // lightweight mapping to keep demo simple
    Business b = new Business();
    b.setName((String) body.get("name"));
    b.setDescription((String) body.get("description"));
    b.setCategories((List<String>) body.getOrDefault("categories", List.of()));
    b.setAddress((String) body.get("address"));
    b.setPhone((String) body.get("phone"));
    b.setWebsite((String) body.get("website"));
    Object rating = body.get("rating");
    if (rating != null) b.setRating(Double.valueOf(rating.toString()));
    if (body.containsKey("lat") && body.containsKey("lon")) {
      double lat = Double.parseDouble(body.get("lat").toString());
      double lon = Double.parseDouble(body.get("lon").toString());
      b.setLocation(new org.springframework.data.geo.Point(lon, lat));
    }
    return service.save(b);
  }

  @GetMapping("/{id}")
  public Business get(@PathVariable String id) {
    Business b = service.get(id);
    if (b == null) throw new BusinessNotFound();
    return b;
  }

  @GetMapping("/search")
  public Page<Business> search(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) Double lat,
      @RequestParam(required = false) Double lon,
      @RequestParam(required = false, name = "radius_km") String radiusKm,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
      @RequestParam(defaultValue = "false") boolean sortByDistance
  ) {
    return service.search(q, lat, lon, radiusKm, page, size, sortByDistance);
  }

  @ResponseStatus(HttpStatus.NOT_FOUND)
  private static class BusinessNotFound extends RuntimeException {}
}
```

---

## bootstrap/SampleDataLoader.java
```java
package com.example.businesssearch.bootstrap;

import com.example.businesssearch.model.Business;
import com.example.businesssearch.repo.BusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@Order(1)
@Profile("!prod")
@RequiredArgsConstructor
public class SampleDataLoader implements CommandLineRunner {
  private final BusinessRepository repo;

  @Override
  public void run(String... args) {
    if (repo.count() > 0) return;

    // A few sample records (coords roughly around Bangalore)
    repo.saveAll(List.of(
      Business.builder().id(UUID.randomUUID().toString())
        .name("Sunrise Cafe").description("Cozy coffee & brunch spot")
        .categories(List.of("cafe","breakfast","coffee"))
        .address("MG Road, Bengaluru")
        .location(new Point(77.612, 12.975))
        .phone("+91-9876543210").website("https://sunrisecafe.example.com")
        .rating(4.3).build(),

      Business.builder().id(UUID.randomUUID().toString())
        .name("TechFix Solutions").description("Laptop & phone repairs")
        .categories(List.of("electronics","repair"))
        .address("Koramangala, Bengaluru")
        .location(new Point(77.628, 12.935))
        .phone("+91-9900011111").website("https://techfix.example.com")
        .rating(4.6).build(),

      Business.builder().id(UUID.randomUUID().toString())
        .name("Spice Route Restaurant").description("Authentic Indian cuisine")
        .categories(List.of("restaurant","indian"))
        .address("Indiranagar, Bengaluru")
        .location(new Point(77.640, 12.971))
        .phone("+91-9988776655").website("https://spiceroute.example.com")
        .rating(4.5).build(),

      Business.builder().id(UUID.randomUUID().toString())
        .name("GreenLeaf Grocers").description("Organic produce & daily needs")
        .categories(List.of("grocery","organic"))
        .address("HSR Layout, Bengaluru")
        .location(new Point(77.651, 12.912))
        .phone("+91-9123456780").website("https://greenleaf.example.com")
        .rating(4.1).build()
    ));
  }
}
```

---

## Dockerfile (for the app)
```dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/business-search-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

---

## README.md (Quick start)
```md
# Business Search Demo

### 1) Build the app
mvn -q -DskipTests package

### 2) Boot everything with Docker
# First time: build the app image locally
docker compose build app
# Start ES + Kibana + App
docker compose up

Wait until Kibana says "Server running" and the app is listening on 0.0.0.0:8080.

### 3) Try the API
# List search (keyword only)
curl "http://localhost:8080/api/businesses/search?q=cafe"

# Search within 5 km of a point (Bengaluru center) and sort by distance
curl "http://localhost:8080/api/businesses/search?lat=12.9716&lon=77.5946&radius_km=5&sortByDistance=true"

# Combined keyword + geo
curl "http://localhost:8080/api/businesses/search?q=repair&lat=12.93&lon=77.62&radius_km=6&sortByDistance=true"

# Get details by id
curl "http://localhost:8080/api/businesses/{id}"

# Create a new business
curl -X POST http://localhost:8080/api/businesses \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Blue Bottle Coffee",
    "description": "Specialty coffee bar",
    "categories": ["cafe","coffee"],
    "address": "Church St, Bengaluru",
    "lat": 12.974,
    "lon": 77.605,
    "phone": "+91-9000000000",
    "website": "https://bluebottle.example.com",
    "rating": 4.7
  }'

### 4) Explore data in Kibana
Open http://localhost:5601 → Dev Tools → run:
```
GET /businesses/_search
{
  "query": {
    "match": { "name": "cafe" }
  }
}
```

### Notes
- Index name is `businesses`. Spring Data will auto-create mapping from annotations. For production, define an index template and pipeline for analyzers/normalizers as needed.
- Geo field is `location` (geo_point). Spring `Point(lon, lat)` – mind the order.
- For fuzziness, boosting, or multi-match tuning, switch to `NativeQuery` with query DSL.
```

---

## Optional: NativeQuery example (full-text + geo + fuzziness)
```java
NativeQuery query = NativeQuery.builder()
  .withQuery(q -> q
    .bool(b -> b
      .must(m -> m.multiMatch(mm -> mm
        .query(keyword)
        .fields("name^3", "description", "categories")
        .fuzziness("AUTO")))
      .filter(f -> f.geoDistance(g -> g
        .field("location")
        .location(l -> l.lat(lat).lon(lon))
        .distance(radiusKm + "km")))
    )
  )
  .withPageable(PageRequest.of(page, size))
  .build();
```

---

## Hardening tips (prod)
- Run ES with security and TLS, create a dedicated user for the app.
- Manage index versioning and aliases, e.g., `businesses-v1` + `businesses` alias.
- Add analyzers (edge n-grams) for typeahead.
- Cache recent queries in Redis if needed.
- Add request validation and DTO mappers; use MapStruct.
- Add rate limiting on the API.
```

