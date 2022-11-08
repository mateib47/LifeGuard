package com.lifeguard.person;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PersonService {
    private PersonRepository personRepository;

    public Long addPerson(PersonRequest personRequest) {
        Person person = new Person(personRequest.getFirstName(),
                personRequest.getLastName(),
                personRequest.getEmail(),
                personRequest.getPhoneNumber());
        personRepository.save(person);
        return person.getId();
    }
}
