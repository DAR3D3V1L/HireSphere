package com.jobportal.controllers;

import com.jobportal.entity.*;
import com.jobportal.repository.JobSeekerApplyRepository;
import com.jobportal.repository.JobSeekerSaveRepository;
import com.jobportal.services.JobPostActivityService;
import com.jobportal.services.UsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@Controller
public class JobPostActivityController {

    private final UsersService usersService;
    private final JobPostActivityService jobPostActivityService;
    private final JobSeekerApplyRepository jobSeekerApplyRepository;
    private final JobSeekerSaveRepository jobSeekerSaveRepository;

    @Autowired
    public JobPostActivityController(UsersService usersService,
                                     JobPostActivityService jobPostActivityService,
                                     JobSeekerApplyRepository jobSeekerApplyRepository,
                                     JobSeekerSaveRepository jobSeekerSaveRepository) {
        this.usersService = usersService;
        this.jobPostActivityService = jobPostActivityService;
        this.jobSeekerApplyRepository = jobSeekerApplyRepository;
        this.jobSeekerSaveRepository = jobSeekerSaveRepository;
    }

    /**
     * Main dashboard / search results page.
     * - Recruiters: see their own posted jobs with candidate counts
     * - Job Seekers: see all jobs with applied/saved flags; supports keyword search
     */
    @GetMapping("/dashboard/")
    public String searchJobs(@RequestParam(value = "job", required = false) String job,
                             @RequestParam(value = "location", required = false) String location,
                             Model model) {

        Object currentUserProfile = usersService.getCurrentUserProfile();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            String currentUsername = authentication.getName();
            model.addAttribute("username", currentUsername);

            if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("Recruiter"))) {
                // Load recruiter's own jobs
                List<RecruiterJobsDto> recruiterJobs = jobPostActivityService
                        .getRecruiterJobs(((RecruiterProfile) currentUserProfile).getUserAccountId());
                model.addAttribute("jobPost", recruiterJobs);

            } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("Job Seeker"))) {
                // Load all jobs with apply/save status flags
                JobSeekerProfile seekerProfile = (JobSeekerProfile) currentUserProfile;
                List<JobPostActivity> jobPost = jobPostActivityService.search(job, location);

                // Mark isActive (applied) and isSaved flags per job
                for (JobPostActivity jobPostActivity : jobPost) {
                    jobPostActivity.setIsActive(jobSeekerApplyRepository.existsByUserIdAndJob(seekerProfile, jobPostActivity));
                    jobPostActivity.setIsSaved(jobSeekerSaveRepository.existsByUserIdAndJob(seekerProfile, jobPostActivity));
                }
                model.addAttribute("jobPost", jobPost);
                model.addAttribute("job", job);
                model.addAttribute("location", location);
            }
        }

        model.addAttribute("user", currentUserProfile);
        return "dashboard";
    }

    @GetMapping("/dashboard/add")
    public String addJobs(Model model) {
        model.addAttribute("jobPostActivity", new JobPostActivity());
        model.addAttribute("user", usersService.getCurrentUserProfile());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
            model.addAttribute("username", authentication.getName());
        }
        return "add-jobs";
    }

    @PostMapping("/dashboard/addNew")
    public String addNew(JobPostActivity jobPostActivity, Model model) {
        Users user = usersService.getCurrentUser();
        if (user != null) {
            jobPostActivity.setPostedById(user);
        }
        jobPostActivity.setPostedDate(new Date());
        model.addAttribute("jobPostActivity", jobPostActivity);
        jobPostActivityService.addNew(jobPostActivity);
        return "redirect:/dashboard/";
    }

    @GetMapping("dashboard/edit/{id}")
    public String editJob(@PathVariable("id") int id, Model model) {
        JobPostActivity jobPostActivity = jobPostActivityService.getOne(id);
        model.addAttribute("jobPostActivity", jobPostActivity);
        model.addAttribute("user", usersService.getCurrentUserProfile());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
            model.addAttribute("username", authentication.getName());
        }
        return "add-jobs";
    }

    @PostMapping("/dashboard/deleteJob/{id}")
    public String deleteJob(@PathVariable("id") int id) {
        jobPostActivityService.deleteById(id);
        return "redirect:/dashboard/";
    }
}
