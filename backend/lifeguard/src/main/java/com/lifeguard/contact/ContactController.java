package com.lifeguard.contact;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "api/v1/contact")
@AllArgsConstructor
public class ContactController {
    private ContactService contactService;
//fix enable cors and make a proxy
    @CrossOrigin
    @PostMapping(path = "add")
    public String sendEmail(@RequestBody ContactRequest contactRequest){
        return contactService.sendEmail(contactRequest);
    }
}
