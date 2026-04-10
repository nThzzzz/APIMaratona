package com.APImaratona.Maratona.DTO.Codeforces;

import lombok.Data;
import java.util.List;

@Data
public class CodeforcesResponseDTO {
    private String status;
    private List<CodeforcesSubmissionDTO> result;
}
