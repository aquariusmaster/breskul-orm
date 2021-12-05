package com.anderb.breskulorm;

import com.anderb.breskulorm.annotation.*;
import lombok.Data;

import static com.anderb.breskulorm.annotation.GenerationType.IDENTITY;

@Data
@Entity
@Table("address")
public class Address {
    @Id(generatedValue = IDENTITY)
    private Long id;

    @Column("address_line")
    private String addressLine;

    @Column("city")
    private String city;

}
