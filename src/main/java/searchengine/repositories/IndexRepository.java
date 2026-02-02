package searchengine.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexEntity;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Long> {

    @Query(value = "SELECT * FROM `indexing` WHERE lemma_id = :lemmaId", nativeQuery = true)
    List<IndexEntity> findByLemmaId(Long lemmaId);
}
