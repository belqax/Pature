package app.belqax.pature.data.network;

import app.belqax.pature.data.repository.AuthRepository.EmailRegisterRequestDto;
import app.belqax.pature.data.repository.AuthRepository.EmailVerificationConfirmRequestDto;
import app.belqax.pature.data.repository.AuthRepository.LogoutRequestDto;
import app.belqax.pature.data.repository.AuthRepository.PasswordForgotRequestDto;
import app.belqax.pature.data.repository.AuthRepository.PasswordResetRequestDto;
import app.belqax.pature.data.repository.AuthRepository.ResendVerificationEmailRequestDto;
import app.belqax.pature.data.repository.AuthRepository.SimpleDetailResponseDto;
import app.belqax.pature.data.repository.AuthRepository.TokenPairDto;
import app.belqax.pature.data.repository.AuthRepository.UserLoginRequestDto;
import app.belqax.pature.data.repository.AuthRepository.UserRefreshRequestDto;
import app.belqax.pature.data.repository.AuthRepository.PasswordChangeRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApi {

    @POST("auth/login")
    Call<TokenPairDto> login(@Body UserLoginRequestDto body);

    @POST("auth/register")
    Call<SimpleDetailResponseDto> register(@Body EmailRegisterRequestDto body);

    @POST("auth/email/resend")
    Call<SimpleDetailResponseDto> resendVerificationEmail(@Body ResendVerificationEmailRequestDto body);

    @POST("auth/register/confirm")
    Call<SimpleDetailResponseDto> confirmEmail(@Body EmailVerificationConfirmRequestDto body);

    @POST("auth/password/forgot")
    Call<SimpleDetailResponseDto> passwordForgot(@Body PasswordForgotRequestDto body);

    @POST("auth/password/reset")
    Call<SimpleDetailResponseDto> passwordReset(@Body PasswordResetRequestDto body);
    @POST("auth/password/change")
    Call<SimpleDetailResponseDto> changePassword(@Body PasswordChangeRequest body);
    @POST("auth/logout")
    Call<SimpleDetailResponseDto> logout(@Body LogoutRequestDto body);

    @POST("auth/refresh")
    Call<TokenPairDto> refresh(@Body UserRefreshRequestDto body);
}
