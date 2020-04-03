package com.lukk.sky.user.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.Set;


@Data
// lombok's  @Getter @Setter @HashCodeAndEquals @RequiredArgsConstructor (final args) @ToString
@Entity(name = "User")    // Spring's entity name
@Table(name = "user")     // DB table name (without it will be same as entity name)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Expose
    private Long id;

    //    @NotBlank
//    @Email
    @Column(nullable = false, unique = true, length = 100)
    @JsonProperty("email")
    @Expose
    private String email;

    //    @NotBlank
    private String password;

    @ManyToMany
    @Fetch(FetchMode.JOIN)
    @JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles;
//
//    @OneToMany(mappedBy = "receiver")
//    @JsonIgnore
//    @ToString.Exclude
//    private List<Message> receivedMessage;
//
//    @OneToMany(mappedBy = "sender")
//    @JsonIgnore
//    @ToString.Exclude
//    private List<Message> sentMessage;
//
//    @OneToMany(mappedBy = "user")
//    private List<Booked> booked;

}
