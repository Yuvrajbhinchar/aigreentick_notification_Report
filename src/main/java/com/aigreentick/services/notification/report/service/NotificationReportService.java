package com.aigreentick.services.notification.report.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import com.aigreentick.services.notification.report.dto.ChannelSummary;
import com.aigreentick.services.notification.report.dto.DailyVolumeReportDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationReportService {

    private final MongoTemplate mongoTemplate;
    private final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    public List<DailyVolumeReportDTO> getDailyVolumeReport(LocalDate start, LocalDate end) {

        Map<String, ChannelSummary> emailData = aggregateDaily("email_notification", start, end);
        Map<String, ChannelSummary> pushData = aggregateDaily("push_notification", start, end);

        // Merge all days
        Set<String> allDates = new TreeSet<>();
        allDates.addAll(emailData.keySet());
        allDates.addAll(pushData.keySet());

        List<DailyVolumeReportDTO> out = new ArrayList<>();

        for (String day : allDates) {
            DailyVolumeReportDTO dto = new DailyVolumeReportDTO();
            dto.setDate(day);

            ChannelSummary email = emailData.getOrDefault(day, new ChannelSummary());
            ChannelSummary push = pushData.getOrDefault(day, new ChannelSummary());

            dto.setEmail(email);
            dto.setPush(push);
            dto.setTotal(email.getTotal() + push.getTotal());

            out.add(dto);
        }

        return out;
    }

    @SuppressWarnings("unchecked")
    private Map<String, ChannelSummary> aggregateDaily(String collection, LocalDate start, LocalDate end) {

        Date startDate = Date.from(start.atStartOfDay(ZONE).toInstant());
        Date endDate = Date.from(end.plusDays(1).atStartOfDay(ZONE).toInstant()); // exclusive

        MatchOperation match = Aggregation.match(
                Criteria.where("createdAt")
                        .gte(startDate)
                        .lt(endDate)
        );

        ProjectionOperation project = Aggregation.project()
                .andExpression("dateToString('%Y-%m-%d', $createdAt)").as("day")
                .and("status").as("status");

        GroupOperation group1 = Aggregation.group("day", "status")
                .count().as("count");

        GroupOperation group2 = Aggregation.group("_id.day")
                .push(new Document("status", "$_id.status")
                        .append("count", "$count"))
                .as("statusCounts")
                .sum("count").as("total");

        SortOperation sort = Aggregation.sort(Sort.by(Sort.Direction.ASC, "_id"));

        Aggregation agg = Aggregation.newAggregation(
                match,
                project,
                group1,
                group2,
                sort
        );

        List<Document> raw = mongoTemplate
                .aggregate(agg, collection, Document.class)
                .getMappedResults();

        Map<String, ChannelSummary> map = new HashMap<>();

        for (Document doc : raw) {
            String day = doc.getString("_id");
            ChannelSummary summary = new ChannelSummary();

            Number totalNumber = (Number) doc.get("total");
            summary.setTotal(totalNumber != null ? totalNumber.longValue() : 0L);

            Map<String, Long> statusMap = new HashMap<>();
            List<Document> list = (List<Document>) doc.get("statusCounts");

            if (list != null) {
                for (Document d : list) {
                    Number countNumber = (Number) d.get("count");
                    statusMap.put(d.getString("status"), countNumber != null ? countNumber.longValue() : 0L);
                }
            }

            summary.setStatusCounts(statusMap);
            map.put(day, summary);
        }

        return map;
    }
}
