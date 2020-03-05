package com.lukk.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Set;


@Data // lombok's  @Getter @Setter @HashCodeAndEquals @RequiredArgsConstructor (final args) @ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "User")        // Spring's entity name
@Table(name = "user")     // DB table name (without it will be same as entity name)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Expose
    private Long id;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true, length = 100)
    @JsonProperty("email")
    @Expose
    private String email;

    @NotBlank
    @JsonProperty("password")
    private String password;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles;

    @OneToMany(mappedBy = "receiver")
    @JsonIgnore
    private List<Message> receivedMessage;

    @OneToMany(mappedBy = "sender")
    @JsonIgnore
    private List<Message> sentMessage;

}
