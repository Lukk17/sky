package com.basic.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Set;

@Data                       // lombok's  @Getter @Setter @HashCodeAndEquals @RequiredArgsConstructor (final args) @ToString
@AllArgsConstructor         // lombok's constructor with all arguments
@NoArgsConstructor          // lombok's constructor with no arguments
@Entity(name="User")        // Spring's entity name
@Table (name = "user")     // DB table name (without it will be same as entity name)
public class User
{

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Setter(AccessLevel.PROTECTED)
    private Long id;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true, length = 100)
    @JsonProperty("email")
    @Setter(AccessLevel.PROTECTED)
    private String email;

    @NotBlank
    @JsonProperty("password")
    private String password;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles;

    @OneToMany(mappedBy = "receiver")
    @Setter(AccessLevel.PROTECTED)
    private List<Message> receivedMessage;

    @OneToMany(mappedBy = "sender")
    @Setter(AccessLevel.PROTECTED)
    private List<Message> sentMessage;

}
