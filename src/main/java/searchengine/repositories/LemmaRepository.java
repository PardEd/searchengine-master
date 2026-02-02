package searchengine.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.LemmaEntity;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Long> {

    @Query(value = "SELECT * FROM lemma WHERE lemma LIKE :lemma AND site_id = :siteId LIMIT 1", nativeQuery = true)
    LemmaEntity findLemmaByNameAndSiteId(String lemma, Long siteId);

    @Query(value = "SELECT id FROM lemma WHERE lemma LIKE :lemma AND site_id = :siteId LIMIT 1", nativeQuery = true)
    Long findIdLemmaByNameAndSiteId(String lemma, Long siteId);

    @Query(value = "SELECT lemma FROM lemma ORDER BY frequency DESC LIMIT :limit", nativeQuery = true)
    List<String> getTopLemma(int limit);

    @Transactional
    @Modifying
    @Query(value = "UPDATE lemma SET frequency = frequency + 1 WHERE id IN (:lemmaIds)", nativeQuery = true)
    void incrementFrequency(List<Long> lemmaIds);

    @Query(value = "SELECT COUNT(*) FROM lemma", nativeQuery = true)
    Integer getSizeLemmas();

    @Query(value = "SELECT COUNT(*) FROM lemma WHERE site_id = :siteId", nativeQuery = true)
    Integer getSizeLemmasBySiteId(Long siteId);
}
