package org.nr31.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Recurrence {
    private String frequency;
    private Integer interval;
    private Integer count;
    private String until;
    private List<String> byDay;
}