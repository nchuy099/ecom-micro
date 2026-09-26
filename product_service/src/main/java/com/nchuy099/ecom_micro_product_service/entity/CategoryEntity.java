package com.nchuy099.ecom_micro_product_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Table(name = "categories")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CategoryEntity extends BaseEntity {

    @Id
    @GeneratedValue(generator = "uuid")
    private String id;

    private String name;

    @Column(name = "parent_id")
    private String parentId;
}
