package project.fitnessapplication.template.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TemplateForm {
    @NotBlank(message = "Template name is required")
    private String name;

    @Valid
    private List<TemplateItemForm> items = new ArrayList<>();
}
