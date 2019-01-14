package com.basic.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Entity(name="users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true, length = 100)
    @JsonProperty("email") private String email;

    @NotBlank
    @JsonProperty("password") private String password;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles;

    @OneToMany
    private List<Message> receivedMessage;

    @OneToMany
    private List<Message> sentMessage;

    public User(@NotBlank @Email String email, @NotBlank String password) {
        this.email = email;
        this.password = password;
    }

    public User() {
    }


    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) { this.password = password; }

    public void setRoles(Set<Role> roles) { this.roles = roles; }

    //TODO setters and getter with lombok
}
