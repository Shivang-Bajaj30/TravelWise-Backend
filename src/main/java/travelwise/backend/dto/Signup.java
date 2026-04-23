package travelwise.backend.dto;

import lombok.Data;

@Data
public class Signup {
    private String name;
    private String email;
    private String password;
}
