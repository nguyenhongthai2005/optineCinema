package com.opticine.repository;

import com.opticine.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long>, JpaSpecificationExecutor<Movie> {
    Optional<Movie> findByTitle(String title);
    List<Movie> findByStatusOrderByPriorityAscTitleAsc(String status);
    List<Movie> findByStatusInOrderByPriorityAscTitleAsc(List<String> statuses);
    List<Movie> findByStatusOrderByPopularityScoreDescTitleAsc(String status, Pageable pageable);
}
