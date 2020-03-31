package com.lukk.controller;

import com.lukk.dto.UserDTO;
import com.lukk.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


//CrossOrigin allow CORS from Angular App running at the specified URL.
@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
@RestController
@RequiredArgsConstructor
public class UserController {

    final UserService userService;

    @PostMapping("/registration")
    public ResponseEntity<?> register(@RequestBody UserDTO newUser) {

        userService.saveUser(newUser);
        return ResponseEntity.accepted().build();
    }

//    @RequestMapping(value = "/login", method = {RequestMethod.OPTIONS, RequestMethod.GET})
//    public ResponseEntity<?> login() {
//        // get logged user details from spring security
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        String userEmail = auth.getName();
//        User user = userService.findByUserEmail(userEmail);
//
//        // build Json only from exposed in User entity fields (id and email)
//        // instead sending back password and other not required by front-end fields
//        Gson gson = new GsonBuilder()
//                .excludeFieldsWithoutExposeAnnotation()
//                .create();
//        String userDetails = gson.toJson(user);
//
//        return new ResponseEntity<>(userDetails, HttpStatus.OK);
//    }

    @GetMapping("/userList")
    public ResponseEntity<List<UserDTO>> userList() {
        return ResponseEntity.ok(userService.findAllAndConvertToDTO());
    }

//    @GetMapping("/userDetails")
//    public ResponseEntity<UserDTO> userDetails(Authentication auth){
//        return ResponseEntity.ok(userService.findUserDetails(auth.getName()));
//    }
}
