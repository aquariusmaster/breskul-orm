package com.anderb.breskulorm;

import com.anderb.breskulorm.annotation.Column;
import com.anderb.breskulorm.annotation.Entity;
import com.anderb.breskulorm.annotation.Id;
import com.anderb.breskulorm.annotation.Table;
import lombok.Data;

@Data
@Entity
@Table("persons")
public class Person {
    @Id
    private Long id;

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

}
