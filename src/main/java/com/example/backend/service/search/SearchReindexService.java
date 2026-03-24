package com.example.backend.service.search;

import com.example.backend.config.search.SearchElasticProperties;
import com.example.backend.controller.search.dto.SearchReindexResponse;
import com.example.backend.repository.message.MessageCapsuleRepository;
import com.example.backend.repository.message.MessageRepository;
import com.example.backend.repository.pin.ConversationPinRepository;
import com.example.backend.repository.post.PostRepository;
import com.example.backend.search.index.SearchIndexingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchReindexService {

    private final SearchIndexingService searchIndexingService;
    private final SearchElasticProperties searchElasticProperties;

    private final MessageRepository messageRepository;
    private final ConversationPinRepository conversationPinRepository;
    private final MessageCapsuleRepository messageCapsuleRepository;
    private final PostRepository postRepository;

    public SearchReindexResponse reindexAll(UUID requestedBy, Integer batchSizeRaw) {
        int batchSize = normalizeBatchSize(batchSizeRaw);
        LocalDateTime requestedAt = LocalDateTime.now();
        long startedAt = System.currentTimeMillis();

        log.info("search reindex started. requestedBy={} batchSize={} elasticReady={} indexName={}",
                requestedBy, batchSize, searchIndexingService.elasticReady(), searchElasticProperties.getIndexName());

        CountResult messageCounts = reindexMessages(batchSize);
        CountResult actionCounts = reindexPins(batchSize);
        CountResult capsuleCounts = reindexCapsules(batchSize);
        CountResult postCounts = reindexPosts(batchSize);

        long durationMillis = System.currentTimeMillis() - startedAt;
        long totalIndexed = messageCounts.indexed + actionCounts.indexed + capsuleCounts.indexed + postCounts.indexed;
        long totalSkipped = messageCounts.skipped + actionCounts.skipped + capsuleCounts.skipped + postCounts.skipped;

        log.info("search reindex finished. requestedBy={} durationMillis={} messages={}/{} actions={}/{} capsules={}/{} posts={}/{} totals={}/{} backend={}",
                requestedBy,
                durationMillis,
                messageCounts.indexed, messageCounts.skipped,
                actionCounts.indexed, actionCounts.skipped,
                capsuleCounts.indexed, capsuleCounts.skipped,
                postCounts.indexed, postCounts.skipped,
                totalIndexed, totalSkipped,
                searchIndexingService.elasticReady() ? "elasticsearch" : "db-fallback");

        return new SearchReindexResponse(
                searchIndexingService.elasticReady(),
                searchIndexingService.elasticReady() ? "elasticsearch" : "db-fallback",
                searchElasticProperties.getIndexName(),
                batchSize,
                requestedBy,
                requestedAt,
                durationMillis,
                new SearchReindexResponse.Counts(messageCounts.indexed, messageCounts.skipped),
                new SearchReindexResponse.Counts(actionCounts.indexed, actionCounts.skipped),
                new SearchReindexResponse.Counts(capsuleCounts.indexed, capsuleCounts.skipped),
                new SearchReindexResponse.Counts(postCounts.indexed, postCounts.skipped),
                new SearchReindexResponse.Totals(totalIndexed, totalSkipped)
        );
    }

    private CountResult reindexMessages(int batchSize) {
        return reindexPaged(
                "messages",
                pageable -> messageRepository.findAll(pageable),
                batchSize,
                message -> !message.isDeleted(),
                searchIndexingService::indexMessage
        );
    }

    private CountResult reindexPins(int batchSize) {
        return reindexPaged(
                "actions",
                pageable -> conversationPinRepository.findAll(pageable),
                batchSize,
                pin -> !pin.isDeleted(),
                searchIndexingService::indexPin
        );
    }

    private CountResult reindexCapsules(int batchSize) {
        return reindexPaged(
                "capsules",
                pageable -> messageCapsuleRepository.findAll(pageable),
                batchSize,
                capsule -> true,
                searchIndexingService::indexCapsule
        );
    }

    private CountResult reindexPosts(int batchSize) {
        return reindexPaged(
                "posts",
                pageable -> postRepository.findAll(pageable),
                batchSize,
                post -> !post.isDeleted(),
                searchIndexingService::indexPost
        );
    }

    private <T> CountResult reindexPaged(
            String label,
            Function<Pageable, Page<T>> loader,
            int batchSize,
            Predicate<T> includer,
            Consumer<T> indexer
    ) {
        int pageNumber = 0;
        long indexed = 0;
        long skipped = 0;

        while (true) {
            Page<T> page = loader.apply(PageRequest.of(pageNumber, batchSize, Sort.by(Sort.Direction.ASC, "id")));

            for (T item : page.getContent()) {
                if (includer.test(item)) {
                    indexer.accept(item);
                    indexed++;
                } else {
                    skipped++;
                }
            }

            log.info("search reindex progress. label={} page={} pageSize={} indexedSoFar={} skippedSoFar={} hasNext={}",
                    label, pageNumber, page.getNumberOfElements(), indexed, skipped, page.hasNext());

            if (!page.hasNext()) {
                break;
            }
            pageNumber++;
        }

        return new CountResult(indexed, skipped);
    }

    private int normalizeBatchSize(Integer raw) {
        int value = raw == null ? 300 : raw;
        if (value < 10) return 10;
        return Math.min(value, 1000);
    }

    private record CountResult(long indexed, long skipped) {}
}