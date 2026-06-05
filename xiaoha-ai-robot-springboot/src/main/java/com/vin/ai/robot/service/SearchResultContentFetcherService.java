package com.vin.ai.robot.service;

import com.vin.ai.robot.model.dto.SearchResultDTO;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public interface SearchResultContentFetcherService {

    public CompletableFuture<List<SearchResultDTO>> batchFetch(List<SearchResultDTO> searchResults, long timeout, TimeUnit unit);
}
