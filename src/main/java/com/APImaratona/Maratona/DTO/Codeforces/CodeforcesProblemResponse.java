package com.APImaratona.Maratona.DTO.Codeforces;

import lombok.Data;

import java.util.List;

@Data
public class CodeforcesProblemResponse {
    private Integer contestId;
    private String index;
    private String name;
    private List<String> tags;
    private int rating;
}
