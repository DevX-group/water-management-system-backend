package com.backend.water_management_system.blog.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.backend.water_management_system.service.CloudinaryService;
import com.backend.water_management_system.blog.entity.Blog;
import com.backend.water_management_system.blog.repository.BlogRepository;
import com.backend.water_management_system.dto.CloudinaryUploadResponse;
import java.util.Map;
import java.util.HashMap;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/blogs")
@CrossOrigin(origins = "http://localhost:8080") // Your React port
public class BlogController {

    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        CloudinaryUploadResponse response = cloudinaryService.uploadFile(file);
        Map<String, String> result = new HashMap<>();
        result.put("imageUrl", response.getUrl());
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public List<Blog> getAllBlogs() {
        return blogRepository.findAll();
    }

    @PostMapping
    public Blog createBlog(@RequestBody Blog blog) {
        blog.setDate(LocalDate.now());
        return blogRepository.save(blog);
    }

    @DeleteMapping("/{id}")
    public void deleteBlog(@PathVariable Long id) {
        blogRepository.deleteById(id);
    }
}