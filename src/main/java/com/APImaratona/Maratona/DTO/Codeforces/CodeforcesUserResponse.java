package com.APImaratona.Maratona.DTO.Codeforces;

import lombok.Data;

import java.util.List;

@Data
public class CodeforcesUserResponse {
    private String status;
    private List<CodeforcesUserInfoResponse> result;
}
