package com.jobportal.controllers;

import com.jobportal.services.JobPostActivityService;
import com.jobportal.services.UsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class GlobalSearchController {

    private final JobPostActivityService jobPostActivityService;
    private final UsersService usersService;

    @Autowired
    public GlobalSearchController(JobPostActivityService jobPostActivityService, UsersService usersService) {
        this.jobPostActivityService = jobPostActivityService;
        this.usersService = usersService;
    }

    /**
     * Public search accessible from the home page — redirects to dashboard with keyword
     * if authenticated, otherwise shows results on the index page.
     */
    @GetMapping("/global-search/")
    public String globalSearch(@RequestParam(value = "job", required = false) String job,
                               @RequestParam(value = "location", required = false) String location,
                               Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Authenticated users go to the dashboard search
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
            String redirectUrl = "/dashboard/?";
            if (job != null && !job.isEmpty()) redirectUrl += "job=" + job + "&";
            if (location != null && !location.isEmpty()) redirectUrl += "location=" + location;
            return "redirect:" + redirectUrl;
        }

        // Public: show results on the landing page
        model.addAttribute("jobResults", jobPostActivityService.search(job, location));
        model.addAttribute("job", job);
        model.addAttribute("location", location);
        return "index";
    }
}
