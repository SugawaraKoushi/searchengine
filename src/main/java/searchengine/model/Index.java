package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "\"indexes\"")
public class Index implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private int id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;

    @Column(name = "\"rank\"", nullable = false)
    private float rank;

    @Override
    public int hashCode() {
        int result = id;
        result += result * 31 + Math.round(rank);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Index i) {
            return this.page.getId() == i.page.getId();
        }

        return false;
    }
}
