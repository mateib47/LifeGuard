package com.lifeguard.person;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "api/v1/user")
@AllArgsConstructor
public class PersonController {
    private PersonService personService;

    @PostMapping(path = "add")
    public Long addPerson(@RequestBody PersonRequest personRequest){
        return personService.addPerson(personRequest);
    }
}
