package com.jobportal.repository;

import com.jobportal.entity.IRecruiterJobs;
import com.jobportal.entity.JobPostActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobPostActivityRepository extends JpaRepository<JobPostActivity, Integer> {

    @Query(value = " SELECT COUNT(s.user_id) as totalCandidates,j.job_post_id,j.job_title,l.id as locationId,l.city,l.state,l.country,c.id as companyId,c.name FROM job_post_activity j " +
            " inner join job_location l " +
            " on j.job_location_id = l.id " +
            " INNER join job_company c  " +
            " on j.job_company_id = c.id " +
            " left join job_seeker_apply s " +
            " on s.job = j.job_post_id " +
            " where j.posted_by_id = :recruiter " +
            " GROUP By j.job_post_id", nativeQuery = true)
    List<IRecruiterJobs> getRecruiterJobs(@Param("recruiter") int recruiter);

    @Query(value = "SELECT j FROM JobPostActivity j WHERE " +
           "(:job IS NULL OR :job = '' OR LOWER(j.jobTitle) LIKE LOWER(CONCAT('%', :job, '%'))) AND " +
           "(:location IS NULL OR :location = '' OR LOWER(j.jobLocationId.city) LIKE LOWER(CONCAT('%', :location, '%')) " +
           "OR LOWER(j.jobLocationId.state) LIKE LOWER(CONCAT('%', :location, '%')) " +
           "OR LOWER(j.jobLocationId.country) LIKE LOWER(CONCAT('%', :location, '%')))")
    List<JobPostActivity> search(@Param("job") String job, @Param("location") String location);
}