package com.techstore.repository;

import com.techstore.entity.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("CategoryRepository — заявките за каноничен URL на категория, срещу база")
class CategoryRepositoryCanonicalUrlTest {

    @Autowired TestEntityManager entityManager;
    @Autowired CategoryRepository repository;

    private Category laptops;
    private Category gaming;
    private Category hiddenTorches;
    private Category torches;

    private Category category(String slug, boolean show, Category parent) {
        Category c = new Category();
        c.setNameBg(slug);
        c.setSlug(slug);
        c.setShow(show);
        c.setParent(parent);
        return entityManager.persist(c);
    }

    @BeforeEach
    void setUp() {
        laptops = category("laptops", true, null);
        category("gaming-laptops", true, laptops);
        gaming = category("gaming", true, null);
        category("old-gaming-chairs", false, gaming);
        hiddenTorches = category("torches", false, null);
        torches = category("torches", true, null);
        entityManager.flush();
    }

    @Test
    @DisplayName("По slug се намира видимата категория, дори скрита да е със същия slug и по-малко ID")
    void slugResolvesToTheVisibleCategory() {
        assertThat(hiddenTorches.getId()).isLessThan(torches.getId());
        assertThat(repository.findFirstBySlugAndShowTrueOrderByIdAsc("torches")).contains(torches);
        assertThat(repository.findFirstBySlugAndShowTrueOrderByIdAsc("missing")).isEmpty();
    }

    @Test
    @DisplayName("Родител е само категория с видимо дете")
    void parentNeedsAVisibleChild() {
        assertThat(repository.existsByParentIdAndShowTrue(laptops.getId())).isTrue();
        assertThat(repository.existsByParentIdAndShowTrue(gaming.getId())).isFalse();
        assertThat(repository.existsByParentIdAndShowTrue(torches.getId())).isFalse();
        assertThat(repository.findVisibleParentIds()).containsExactly(laptops.getId());
    }
}
