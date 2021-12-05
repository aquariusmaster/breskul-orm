package com.anderb.breskulorm;

import com.anderb.breskulorm.annotation.*;
import lombok.Data;

@Data
@Entity
@Table("persons")
public class Person {
    @Id(generatedValue = GenerationType.SEQUENCE)
    private Long id;

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

}
