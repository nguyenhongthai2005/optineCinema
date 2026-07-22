package com.opticine.dto.admin.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomStatusRequest {

    @NotBlank
    @Size(max = 50)
    private String status;
}
