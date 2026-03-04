package ticket.exercise.pulsedesk.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {

    @NotBlank(message = "Comment content must not be blank")
    @Size(min = 5, max = 2000, message = "Comment must be between 5 and 2000 characters")
    private String content;

    @NotBlank(message = "Source must not be blank")
    private String source;
}
