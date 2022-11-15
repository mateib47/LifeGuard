package com.lifeguard.person;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.Optional;

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
            personRepository.save(person);
            return person.getId();
        }else{
            return existingLifeguardUser.get().getId();
        }
    }
}
