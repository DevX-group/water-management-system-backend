package com.backend.water_management_system.blog.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.backend.water_management_system.blog.entity.Blog;
import com.backend.water_management_system.blog.repository.BlogRepository;
import com.backend.water_management_system.payments.dto.CloudinaryUploadResponse;
import com.backend.water_management_system.payments.service.CloudinaryService;

@RestController
@RequestMapping("/api/blogs")
@CrossOrigin(origins = "http://localhost:8080") // frontend port
public class BlogController {

    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @PostMapping("/upload-image")  // Endpoint to handle image uploads for blog posts
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        CloudinaryUploadResponse response = cloudinaryService.uploadFile(file);
        Map<String, String> result = new HashMap<>();
        result.put("imageUrl", response.getUrl());
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public List<Blog> getAllBlogs() {    // Retrieve all blog posts from the database
        return blogRepository.findAll();
    }

    @PostMapping        // Create a new blog post with the current date
    public Blog createBlog(@RequestBody Blog blog) {
        blog.setDate(LocalDate.now());
        return blogRepository.save(blog);
    }

    @DeleteMapping("/{id}")         // Delete a blog post by its ID
    public void deleteBlog(@PathVariable Long id) {
        blogRepository.deleteById(id);
    }
}