package com.aigreentick.services.notification.report.service;

import com.aigreentick.services.notification.report.dto.ChannelSummary;
import com.aigreentick.services.notification.report.dto.ProviderStats;
import com.aigreentick.services.notification.report.dto.SummaryReportDTO;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationSummaryService {

    private final MongoTemplate mongoTemplate;
    private final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    public SummaryReportDTO getSummary(LocalDateTime start, LocalDateTime end){

        Date startDate = Date.from(start.atZone(ZONE).toInstant());
        Date endDate = Date.from(end.atZone(ZONE).toInstant());

        // Aggregate for each channel
        ChannelSummary emailSummary = aggregateChannel("email_notification", startDate, endDate);
        ChannelSummary pushSummary  = aggregateChannel("push_notification",  startDate, endDate);

        // overall totals & success rate
        long overallTotal = emailSummary.getTotal() + pushSummary.getTotal();
        double overallSuccessRate = computeOverallSuccessRate(emailSummary, pushSummary);

        // Provider-level aggregation across both collections (merge)
        Map<String, ProviderStats> providers = aggregateProviders(Arrays.asList("email_notification", "push_notification"), startDate, endDate);
        return SummaryReportDTO.builder()
                .start(start.toString())
                .end(end.toString())
                .email(emailSummary)
                .push(pushSummary)
                .overallTotal(overallTotal)
                .overallSuccessRate(overallSuccessRate)
                .topProviders(providers)
                .build();

    }

    private double computeOverallSuccessRate(ChannelSummary email, ChannelSummary push) {
        long successEmail = successCountFromStatusCounts(email.getStatusCounts());
        long successPush  = successCountFromStatusCounts(push.getStatusCounts());
        long total = email.getTotal() + push.getTotal();
        if (total == 0) return 100.0;
        return ( (double)(successEmail + successPush) / (double) total ) * 100.0;
    }

    private long successCountFromStatusCounts(Map<String, Long> statusCounts) {
        if (statusCounts == null || statusCounts.isEmpty()) return 0L;
        // Define what counts as success — adjust as per your NotificationStatus enum
        // Common: SENT, DELIVERED are successes
        long success = 0L;
        for (Map.Entry<String, Long> e : statusCounts.entrySet()) {
            String s = e.getKey();
            if (s == null) continue;
            String sUpper = s.toUpperCase(Locale.ROOT);
            if (sUpper.equals("SENT") || sUpper.equals("DELIVERED") || sUpper.equals("ACCEPTED")) {
                success += e.getValue();
            }
        }
        return success;
    }

    /**
     * Aggregates a single collection into ChannelSummary:
     * - total count
     * - statusCounts map
     * - computed successRate
     */
    private ChannelSummary aggregateChannel(String collection, Date startDate, Date endDate){

        // match
        MatchOperation match = Aggregation.match(
                Criteria.where("createdAt").gte(startDate).lte(endDate)
        );

        // group by status and count
        GroupOperation groupByStatus = Aggregation.group("status").count().as("count");

        Aggregation agg = Aggregation.newAggregation(match, groupByStatus);
        List<Document> raw = mongoTemplate.aggregate(agg, collection, Document.class).getMappedResults();

        Map<String, Long> statusCounts = new HashMap<>();
        long total = 0L;
        for (Document doc : raw) {
            String status = doc.getString("_id");
            Number n = (Number) doc.get("count");
            long count = n == null ? 0L : n.longValue();
            statusCounts.put(status, count);
            total += count;
        }


        double successRate = 100.0;
        if (total > 0) {
            long success = successCountFromStatusCounts(statusCounts);
            successRate = ((double) success / (double) total) * 100.0;
        }

        return ChannelSummary.builder()
                .total(total)
                .statusCounts(statusCounts)
                .successRate(successRate)
                .build();
    }

/**
 * Aggregates provider stats across multiple collections.
 * Returns provider -> ProviderStats(sent, failed)
 */
    private Map<String, ProviderStats> aggregateProviders(List<String> collections, Date startDate, Date endDate) {
         Map<String, ProviderStats> result = new HashMap<>();

        for (String collection : collections) {
        MatchOperation match = Aggregation.match(
                Criteria.where("createdAt").gte(startDate).lte(endDate)
        );

        GroupOperation groupByProviderAndStatus = Aggregation.group("providerType", "status").count().as("count");

        Aggregation agg = Aggregation.newAggregation(match, groupByProviderAndStatus);
        List<Document> raw = mongoTemplate.aggregate(agg, collection, Document.class).getMappedResults();

        for (Document d : raw) {
            Document id = (Document) d.get("_id");
            if (id == null) continue;
            String provider = id.getString("providerType");
            String status = id.getString("status");
            Number n = (Number) d.get("count");
            long c = n == null ? 0L : n.longValue();

            ProviderStats ps = result.getOrDefault(provider, new ProviderStats(0L, 0L));
            long sent = ps.getSent();
            long failed = ps.getFailed();

            // define failed by status name
            String sUpper = status == null ? "" : status.toUpperCase(Locale.ROOT);
            if (sUpper.equals("FAILED") || sUpper.equals("ERROR")) {
                failed += c;
            } else {
                sent += c; // consider other statuses as 'sent/accepted'
            }

            result.put(provider, new ProviderStats(sent, failed));
        }
    }

    // Optionally: sort/limit top providers
     return result.entrySet().stream()
            .sorted(Map.Entry.<String, ProviderStats>comparingByValue(
                    Comparator.comparingLong(ps -> - (ps.getSent() + ps.getFailed()))
            ).reversed()) // keep original order if needed
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                    (a,b) -> a, LinkedHashMap::new));
}

}
