package com.codemash.enrichment.service.ghostoccupations.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ghost")
public class OccupationContoller {

    @GetMapping("/occupation/{ghostName}")
    public Map<String, String> getOccupation(@PathVariable String ghostName) {
        Map<String, String> results = new HashMap<String, String>();
        String result = "occupation";
        switch (ghostName.toLowerCase()) {
            case "mayhem":
                result = "Flying Terror";
                break;
            case "zuul":
                result = "The Gatekeeper of Gozer";
                break;
            case "slimer":
                result = "Disgusting Green Blob";
                break;
            case "vigo":
                result = "16th and 17th-century Moldavian tyrant";
                break;
            case "scoleri brothers":
                result = "Convicted Murderers";
                break;
            case "muncher":
                result = "Dine on anything Metal";
                break;
            case "mini-puffs":
                result = "Being cute and mischievous";
                break;
            case "gertrude aldridge":
                result = "Ghost, Murderer";
                break;
            default:
                break;
        }
        results.put("occupation", result);
        return results;

    }

}
