package org.example.expert.domain.todo.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TodoSearchRequest {

    private String title;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String managerNickname;
}
