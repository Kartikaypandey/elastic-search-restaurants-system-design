package com.example.yelp;


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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean sortByDistance
    ) {
        return service.search(q, lat, lon, radiusKm, page, size, sortByDistance);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    private static class BusinessNotFound extends RuntimeException {}
}
