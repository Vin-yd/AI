package com.vin.ai.robot.service;

import com.vin.ai.robot.model.dto.SearchResultDTO;

import java.util.List;

public interface SearXNGService {

    public List<SearchResultDTO> search(String query);
}
