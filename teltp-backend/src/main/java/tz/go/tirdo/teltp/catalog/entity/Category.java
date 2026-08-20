package tz.go.tirdo.teltp.catalog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tz.go.tirdo.teltp.common.BaseEntity;

import java.util.ArrayList;
import java.util.List;

/** Self-referencing taxonomy. Two levels today (e.g. ICT > Cybersecurity); depth-guarded in the service. */
@Getter
@Setter
@Entity
@Table(name = "categories")
public class Category extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 400)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent")
    private List<Category> children = new ArrayList<>();

    public int depth() {
        return parent == null ? 0 : parent.depth() + 1;
    }
}
