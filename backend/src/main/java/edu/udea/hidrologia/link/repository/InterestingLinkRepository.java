package edu.udea.hidrologia.link.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.udea.hidrologia.link.entity.InterestingLink;

public interface InterestingLinkRepository extends JpaRepository<InterestingLink, Long> {

    List<InterestingLink> findByActiveTrueOrderByDisplayOrderAscIdAsc();
}
