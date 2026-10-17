package com.nexushr.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record InterviewScheduleRequest(
        LocalDate interviewDate,
        LocalTime interviewTime,
        String location,
        String message
) {}
