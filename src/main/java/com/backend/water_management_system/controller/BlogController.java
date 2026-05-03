package com.backend.water_management_system.controller;

import com.backend.water_management_system.entity.Blog;
import com.backend.water_management_system.repository.BlogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/blogs")
@CrossOrigin(origins = "http://localhost:8080") // Your React port
public class BlogController {

    @Autowired
    private BlogRepository blogRepository;

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