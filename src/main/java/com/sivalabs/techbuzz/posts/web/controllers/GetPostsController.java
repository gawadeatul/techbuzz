package com.sivalabs.techbuzz.posts.web.controllers;

import com.sivalabs.techbuzz.common.model.PagedResult;
import com.sivalabs.techbuzz.config.logging.Loggable;
import com.sivalabs.techbuzz.posts.usecases.getposts.GetPostsHandler;
import com.sivalabs.techbuzz.posts.usecases.getposts.PostDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@Loggable
public class GetPostsController {
    private static final Logger logger = LoggerFactory.getLogger(GetPostsController.class);
    private static final String PAGINATION_PREFIX = "paginationPrefix";

    private final GetPostsHandler getPostsHandler;

    public GetPostsController(GetPostsHandler getPostsHandler) {
        this.getPostsHandler = getPostsHandler;
    }

    @GetMapping("/c/{category}")
    public String viewCategory(
            @PathVariable(name = "category") String category,
            @RequestParam(name = "page", defaultValue = "1") Integer page,
            Model model) {
            logger.info("Fetching posts for category {} with page: {}", category, page);
            PagedResult<PostDTO> data = getPostsHandler.getPostsByCategory(category, page);
            model.addAttribute("header", "Posts by category : " + category);
            model.addAttribute(PAGINATION_PREFIX, "/c/" + category+"?");

        model.addAttribute("postsData", data);
        model.addAttribute("categories", getPostsHandler.getAllCategories());
        return "posts";
    }
}
