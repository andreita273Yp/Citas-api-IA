package co.edu.fcv.citas.auth;
import jakarta.validation.constraints.*; import java.util.Set;
public final class AuthDtos {
 private AuthDtos(){}
 public record RegisterRequest(@NotBlank String firstName,@NotBlank String lastName,@NotBlank String documentType,@NotBlank String documentNumber,@NotBlank @Email String email,@NotBlank String phone,@NotBlank @Size(min=8,max=72) String password,Long insurancePlanId){public RegisterRequest(String firstName,String lastName,String documentType,String documentNumber,String email,String phone,String password){this(firstName,lastName,documentType,documentNumber,email,phone,password,null);}}
 public record LoginRequest(@NotBlank @Email String email,@NotBlank String password){}
 public record UserResponse(String id,String firstName,String lastName,String documentType,String documentNumber,String email,String phone,Set<String> roles){}
 public record TokenResponse(String accessToken,String tokenType,long expiresIn){}
}
