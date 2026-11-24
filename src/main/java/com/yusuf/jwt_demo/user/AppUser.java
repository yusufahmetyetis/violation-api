package com.yusuf.jwt_demo.user;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class AppUser {     // Spring'in kendi "User" sınıfıyla karışmasın diye AppUser

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;   // "{bcrypt}$2a$10$..."
    private boolean enabled;
    private String plate;

    protected AppUser() { }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public boolean isEnabled() { return enabled; }
    public String getPlate() { return plate; }
}
