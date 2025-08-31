package com.example.yelp;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.GeoDistanceOrder;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

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
            query.addSort(Sort.by(Sort.Direction.DESC, "_score"));
        }

        SearchHits<Business> searchHits = operations.search(query, Business.class);

        List<Business> businesses = searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        return new PageImpl<>(businesses, query.getPageable(), searchHits.getTotalHits());
    }

}