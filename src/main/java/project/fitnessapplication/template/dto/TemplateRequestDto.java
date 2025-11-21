package project.fitnessapplication.template.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TemplateRequestDto {
    @NotBlank(message = "Name is required")
    private String name;
    private List<TemplateItemDto> items = new ArrayList<>();
}

