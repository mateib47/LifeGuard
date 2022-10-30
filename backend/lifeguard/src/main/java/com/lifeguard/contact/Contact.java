package com.lifeguard.contact;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@Entity
public class Contact {
    @SequenceGenerator(name = "contact_seq",
            sequenceName = "contact_seq",
            allocationSize = 1)
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "contact_seq")
    private Long id;
    private String email;
    private String message;
}
