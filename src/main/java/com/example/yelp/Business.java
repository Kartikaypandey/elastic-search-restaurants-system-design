package com.example.yelp;

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
