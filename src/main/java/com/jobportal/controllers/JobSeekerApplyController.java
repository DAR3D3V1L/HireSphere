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
public class JobSeekerApplyController {

    private final JobPostActivityService jobPostActivityService;
    private final UsersService usersService;
    private final JobSeekerApplyRepository jobSeekerApplyRepository;
    private final JobSeekerSaveRepository jobSeekerSaveRepository;

    @Autowired
    public JobSeekerApplyController(JobPostActivityService jobPostActivityService,
                                    UsersService usersService,
                                    JobSeekerApplyRepository jobSeekerApplyRepository,
                                    JobSeekerSaveRepository jobSeekerSaveRepository) {
        this.jobPostActivityService = jobPostActivityService;
        this.usersService = usersService;
        this.jobSeekerApplyRepository = jobSeekerApplyRepository;
        this.jobSeekerSaveRepository = jobSeekerSaveRepository;
    }

    /**
     * View job details page — loads all required model attributes for both Recruiter and Job Seeker.
     */
    @GetMapping("job-details-apply/{id}")
    public String display(@PathVariable("id") int id, Model model) {
        JobPostActivity jobDetails = jobPostActivityService.getOne(id);
        model.addAttribute("jobDetails", jobDetails);

        Object currentUserProfile = usersService.getCurrentUserProfile();
        model.addAttribute("user", currentUserProfile);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
            model.addAttribute("username", authentication.getName());
        }

        // Recruiter: load candidate applicant list
        if (authentication != null &&
                authentication.getAuthorities().contains(new SimpleGrantedAuthority("Recruiter"))) {
            List<JobSeekerApply> applyList = jobSeekerApplyRepository.findByJob(jobDetails);
            model.addAttribute("applyList", applyList);
        }

        // Job Seeker: check if already applied/saved
        if (authentication != null &&
                authentication.getAuthorities().contains(new SimpleGrantedAuthority("Job Seeker"))) {
            JobSeekerProfile seekerProfile = (JobSeekerProfile) currentUserProfile;
            boolean alreadyApplied = jobSeekerApplyRepository.existsByUserIdAndJob(seekerProfile, jobDetails);
            boolean alreadySaved = jobSeekerSaveRepository.existsByUserIdAndJob(seekerProfile, jobDetails);
            model.addAttribute("alreadyApplied", alreadyApplied);
            model.addAttribute("alreadySaved", alreadySaved);
            model.addAttribute("applyJob", new JobSeekerApply());
        } else {
            model.addAttribute("alreadyApplied", false);
            model.addAttribute("alreadySaved", false);
            model.addAttribute("applyJob", new JobSeekerApply());
        }

        return "job-details";
    }

    /**
     * Apply for a job.
     */
    @PostMapping("/job-details/apply/{id}")
    public String applyJob(@PathVariable("id") int id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }
        JobPostActivity job = jobPostActivityService.getOne(id);
        JobSeekerProfile seekerProfile = (JobSeekerProfile) usersService.getCurrentUserProfile();

        if (!jobSeekerApplyRepository.existsByUserIdAndJob(seekerProfile, job)) {
            JobSeekerApply apply = new JobSeekerApply();
            apply.setUserId(seekerProfile);
            apply.setJob(job);
            apply.setApplyDate(new Date());
            jobSeekerApplyRepository.save(apply);
        }
        return "redirect:/job-details-apply/" + id;
    }

    /**
     * Save / bookmark a job.
     */
    @PostMapping("/job-details/save/{id}")
    public String saveJob(@PathVariable("id") int id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }
        JobPostActivity job = jobPostActivityService.getOne(id);
        JobSeekerProfile seekerProfile = (JobSeekerProfile) usersService.getCurrentUserProfile();

        if (!jobSeekerSaveRepository.existsByUserIdAndJob(seekerProfile, job)) {
            JobSeekerSave save = new JobSeekerSave();
            save.setUserId(seekerProfile);
            save.setJob(job);
            jobSeekerSaveRepository.save(save);
        }
        return "redirect:/job-details-apply/" + id;
    }

    /**
     * View all saved jobs for the logged-in Job Seeker.
     */
    @GetMapping("/saved-jobs/")
    public String savedJobs(Model model) {
        Object currentUserProfile = usersService.getCurrentUserProfile();
        model.addAttribute("user", currentUserProfile);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
            model.addAttribute("username", authentication.getName());
        }

        if (currentUserProfile instanceof JobSeekerProfile seekerProfile) {
            List<JobSeekerSave> savedJobs = jobSeekerSaveRepository.findByUserId(seekerProfile);
            model.addAttribute("savedJobs", savedJobs);
        }
        return "saved-jobs";
    }
}
