package co.edu.usta.spacetrack.vehiculos.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record VehiculoRequest(
        @NotBlank(message = "La placa es obligatoria")
        @Pattern(regexp = "^[A-Za-z]{3}[0-9]{2}[A-Za-z0-9]$", message = "Ingrese una placa colombiana valida")
        String placa,
        @NotBlank(message = "El tipo de vehiculo es obligatorio") @Size(max = 50) String tipoVehiculo,
        @NotBlank(message = "La marca es obligatoria") @Size(max = 50) String marca,
        @NotBlank(message = "El modelo es obligatorio") @Size(max = 50) String modelo,
        @NotNull(message = "El ano modelo es obligatorio") @Min(value = 1950, message = "Ingrese un ano valido") @Max(value = 2100, message = "Ingrese un ano valido") Integer anioModelo,
        @NotNull(message = "La capacidad es obligatoria") @Positive(message = "La capacidad debe ser mayor a cero") Double capacidadCargaKg,
        boolean requiereCadenaFrio,
        boolean activo
) { }
