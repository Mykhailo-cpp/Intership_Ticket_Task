package ticket.exercise.pulsedesk.controller;

import ticket.exercise.pulsedesk.model.dto.request.CommentRequest;
import ticket.exercise.pulsedesk.model.dto.response.ApiResponse;
import ticket.exercise.pulsedesk.model.dto.response.CommentResponse;
import ticket.exercise.pulsedesk.service.CommentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse>> submitComment(
            @Valid @RequestBody CommentRequest request) {

        log.info("POST /comments - source: {}", request.getSource());
        CommentResponse response = commentService.submitComment(request);

        String message = response.isConvertedToTicket()
                ? "Comment submitted and converted to ticket #" + response.getTicketId()
                : "Comment submitted. No action required.";

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, message));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getAllComments() {
        log.info("GET /comments");
        List<CommentResponse> comments = commentService.getAllComments();
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CommentResponse>> getCommentById(@PathVariable Long id) {
        log.info("GET /comments/{}", id);
        CommentResponse comment = commentService.getCommentById(id);
        return ResponseEntity.ok(ApiResponse.success(comment));
    }
}
