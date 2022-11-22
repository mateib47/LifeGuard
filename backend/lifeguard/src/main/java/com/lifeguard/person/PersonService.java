package com.lifeguard.person;

import org.springframework.stereotype.Service;

import java.util.Optional;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class PersonService {
    private PersonRepository personRepository;

    public Long addPerson(PersonRequest personRequest) {
        Optional<LifeguardUser> existingLifeguardUser = personRepository.findByEmail(personRequest.getEmail());
        if(existingLifeguardUser.isEmpty()){
            LifeguardUser person = new LifeguardUser(personRequest.getFirstName(),
                    personRequest.getLastName(),
                    personRequest.getEmail(),
                    personRequest.getPhoneNumber());
//                    , personRequest.getContactEmails());
            personRepository.save(person);
            return person.getId();
        }else{
            return existingLifeguardUser.get().getId();
        }
    }
}
