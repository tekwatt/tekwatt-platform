package com.tekwatt.support.controller;

import com.tekwatt.support.dto.StationReviewRequest;
import com.tekwatt.support.entity.StationReview;
import com.tekwatt.support.repository.StationReviewRepository;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/support/station-reviews")
public class StationReviewController {
    private static final Set<String> STATUSES = Set.of("PENDING", "PUBLISHED", "HIDDEN");
    private final StationReviewRepository reviews;

    public StationReviewController(StationReviewRepository reviews) { this.reviews = reviews; }

    @GetMapping
    public List<StationReview> list(@RequestParam UUID tenantId) {
        return reviews.findAllByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StationReview create(@Valid @RequestBody StationReviewRequest request) {
        return reviews.save(new StationReview(request.tenantId(), request.stationId(), request.customerId(),
                request.customerName(), request.rating(), request.comment()));
    }

    @PatchMapping("/{id}/status")
    @Transactional
    public StationReview moderate(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        String status = Objects.toString(body.get("status"), "").toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(status)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid review status");
        StationReview review = reviews.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Station review not found"));
        review.moderate(status);
        return review;
    }
}
