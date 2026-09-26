package com.nchuy099.ecom_micro_product_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Getter
@Setter
@Table(name = "products")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductEntity extends BaseEntity {

    @Id
    @GeneratedValue(generator = "uuid")
    @UuidGenerator
    private String id;

    private String name;

    private Integer price;

    private Integer stock;

    @Column(name = "category_id")
    private String categoryId;
}
