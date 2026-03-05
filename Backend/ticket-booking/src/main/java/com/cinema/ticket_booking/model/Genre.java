package com.cinema.ticket_booking.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "genres")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", length = 50, unique = true, nullable = false)
    private String name;

    // URL-friendly slug, VD: "action", "romance"
    @Column(name = "slug", length = 50, unique = true, nullable = false)
    private String slug;
}
