package com.APImaratona.Maratona.DTO.Codeforces;

import lombok.Data;

@Data
public class CodeforcesSubmissionResponse {
    private Long id;
    private String verdict;
    private CodeforcesProblemResponse problem;
}
