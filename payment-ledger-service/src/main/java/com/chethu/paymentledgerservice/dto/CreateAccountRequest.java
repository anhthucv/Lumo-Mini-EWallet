package com.chethu.paymentledgerservice.dto;
import jakarta.validation.constraints.NotBlank;
public class CreateAccountRequest {

    @NotBlank(message = "Owner name must not be blank")
    private String ownerName;

    public CreateAccountRequest() {
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

}
