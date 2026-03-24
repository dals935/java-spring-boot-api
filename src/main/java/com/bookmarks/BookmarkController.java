package com.bookmarks;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookmarks")
class BookmarkController {
    private final BookmarkRepository bookmarkRepository;
    BookmarkController(BookmarkRepository bookmarkRepository) {
        this.bookmarkRepository = bookmarkRepository;
    }
    //CRUD API handler methods
    @GetMapping
    List<BookmarkInfo> getBookmarks(){
        return bookmarkRepository.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/{id}")
    ResponseEntity<BookmarkInfo> getBookmarkById(@PathVariable Long id) {
        var bookmark =
                bookmarkRepository.findBookmarkById(id)
                        .orElseThrow(()-> new BookmarkNotFoundException("Bookmark not found"));
        return ResponseEntity.ok(bookmark);
    }

    record CreateBookmarkPayload(
            @NotEmpty(message = "Title is required")
            String title,
            @NotEmpty(message = "Url is required")
            String url) {}

    record UpdateBookmarkPayload(
            @NotEmpty(message = "Title is required")
            String title,
            @NotEmpty(message = "Url is required")
            String url) {
    }

    @PostMapping
    ResponseEntity<Void> createBookmark(
            @Valid @RequestBody CreateBookmarkPayload payload) {
        var bookmark = new Bookmark();
        bookmark.setTitle(payload.title());
        bookmark.setUrl(payload.url());
        bookmark.setCreatedAt(Instant.now());
        var savedBookmark = bookmarkRepository.save(bookmark);
        var url = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .build(savedBookmark.getId());
        return ResponseEntity.created(url).build();
    }

    @PutMapping("/{id}")
    ResponseEntity<Void> updateBookmark(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookmarkPayload payload) {
        var bookmark =
                bookmarkRepository.findById(id)
                        .orElseThrow(()-> new BookmarkNotFoundException("Bookmark not found"));
        bookmark.setTitle(payload.title());
        bookmark.setUrl(payload.url());
        bookmark.setUpdatedAt(Instant.now());
        bookmarkRepository.save(bookmark);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    void deleteBookmark(@PathVariable Long id) {
        var bookmark =
                bookmarkRepository.findById(id)
                        .orElseThrow(()-> new BookmarkNotFoundException("Bookmark not found"));
        bookmarkRepository.delete(bookmark);
    }

    @ExceptionHandler(BookmarkNotFoundException.class)
    ResponseEntity<String> handle(BookmarkNotFoundException e) {
        return ResponseEntity.status(404).body(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity.badRequest().body(errors);
    }

}
