package com.APImaratona.Maratona.DTO.Codeforces;

import lombok.Data;
import java.util.List;

@Data
public class CodeforcesResponse {
    private String status;
    private List<CodeforcesSubmissionResponse> result;
}
