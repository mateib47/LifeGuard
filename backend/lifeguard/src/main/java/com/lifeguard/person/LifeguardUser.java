package com.lifeguard.person;

import javax.persistence.*;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@Entity
public class LifeguardUser {
    @SequenceGenerator(name = "person_seq",
            sequenceName = "person_seq",
            allocationSize = 1)
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "person_seq")
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    @ElementCollection
    private List<String> contactEmails;

    public LifeguardUser(String firstName, String lastName, String email, String phoneNumber, ArrayList<String> contactEmails) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.contactEmails = contactEmails;
    }
}
