package com.chethu.paymentledgerservice.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateAccountRequest {
    @NotBlank(message = "Owner name must not be blank")
    private String ownerName;

    public String getOwnerName(){
        return this.ownerName;
    }

    public void setOwnerName(String ownerName){
        this.ownerName= ownerName;
    }
}
