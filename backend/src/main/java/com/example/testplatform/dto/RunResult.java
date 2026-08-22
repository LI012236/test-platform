package com.example.testplatform.dto;

import com.example.testplatform.model.Run;
import com.example.testplatform.model.RunDetail;

import java.util.List;

public record RunResult(Run run, List<RunDetail> details) {
}
