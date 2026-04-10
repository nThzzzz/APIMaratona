package com.APImaratona.Maratona.DTO.Codeforces;

import lombok.Data;

import java.util.List;

@Data
public class CodeforcesProblemDTO {
    private Integer contestId;
    private String index;
    private String name;
    private List<String> tags;
}
