package com.lifeguard.person;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class PersonRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
//    @ElementCollection
//    private List<String> contactEmails;
}
