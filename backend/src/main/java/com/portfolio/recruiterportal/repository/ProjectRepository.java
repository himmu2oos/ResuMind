package com.portfolio.recruiterportal.repository;

import com.portfolio.recruiterportal.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findAllByOrderBySortOrderAsc();
    List<Project> findByFeaturedTrueOrderBySortOrderAsc();
    List<Project> findByCategoryOrderBySortOrderAsc(String category);
}
