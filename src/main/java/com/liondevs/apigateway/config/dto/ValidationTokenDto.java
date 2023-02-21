package com.liondevs.apigateway.config.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidationTokenDto {
    private String code;

       private Long idUser;
       private  String email;
       private  boolean  isValidToken;

}
