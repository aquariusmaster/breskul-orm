package com.anderb.customormsession;

import com.anderb.customormsession.annotation.Column;
import com.anderb.customormsession.annotation.Entity;
import com.anderb.customormsession.annotation.Id;
import com.anderb.customormsession.annotation.Table;
import lombok.Data;

@Data
@Entity
@Table("persons")
public class Person {
    @Id
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

}
