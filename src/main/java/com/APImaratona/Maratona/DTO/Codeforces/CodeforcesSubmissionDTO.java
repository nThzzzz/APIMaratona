package com.APImaratona.Maratona.DTO.Codeforces;

import lombok.Data;

@Data
public class CodeforcesSubmissionDTO {
    private Long id;
    private String verdict;
    private CodeforcesProblemDTO problem;
}
