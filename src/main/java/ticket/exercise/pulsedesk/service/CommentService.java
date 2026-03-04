package ticket.exercise.pulsedesk.service;

import ticket.exercise.pulsedesk.model.dto.request.CommentRequest;
import ticket.exercise.pulsedesk.model.dto.response.CommentResponse;

import java.util.List;

public interface CommentService {

    CommentResponse submitComment(CommentRequest request);

    List<CommentResponse> getAllComments();

    CommentResponse getCommentById(Long id);
}
