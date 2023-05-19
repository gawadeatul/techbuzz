package com.sivalabs.techbuzz.users.web.controllers;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.sivalabs.techbuzz.notifications.EmailService;
import com.sivalabs.techbuzz.users.domain.dtos.ResendVerificationRequest;
import com.sivalabs.techbuzz.users.domain.dtos.UserDTO;
import com.sivalabs.techbuzz.users.domain.models.User;
import com.sivalabs.techbuzz.users.domain.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Controller
public class ResendVerificationController {

    private static final Logger logger = LoggerFactory.getLogger(ResendVerificationController.class);
    private static final String RESEND_VERIFICATION_EMAIL = "users/resendVerification";

    private final UserService userService;
    private final EmailService emailService;

    public ResendVerificationController(UserService userService, EmailService emailService) {
        this.userService = userService;
        this.emailService = emailService;
    }

    @GetMapping("/resendVerification")
    public String resendVerificationForm(Model model) {
        model.addAttribute("resendEmail", new ResendVerificationRequest(""));
        return RESEND_VERIFICATION_EMAIL;
    }

    @PostMapping("/resendVerification")
    public String resendVerification(
            HttpServletRequest request,
            @Valid @ModelAttribute("resendEmail") ResendVerificationRequest resendVerificationRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return RESEND_VERIFICATION_EMAIL;
        }

        try {
            Optional<User> user = userService.getUserByEmail(resendVerificationRequest.email());
            if (user.isPresent()) {
                // if account is verified then redirect to login page with account exist message
                if (user.get().isVerified()) {
                    redirectAttributes.addFlashAttribute(
                            "errorMessage", "account is already verified, please use forget password if needed");
                    return "redirect:/login";
                }
                // if account is not-verified then resend email and redirect to registrationStatus page
                if (!user.get().isVerified()) {
                    Optional<UserDTO> existingUserDTO =
                            userService.getUserDTO(user.get().getEmail());

                    String baseUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                            .replacePath(null)
                            .build()
                            .toUriString();

                    this.sendVerificationEmail(request, existingUserDTO.get());
                    redirectAttributes.addFlashAttribute(
                            "message",
                            "reset verification link is sent on your provided email ID please check your email");
                    return "redirect:/registrationStatus";
                }
            }
        } catch (Exception e) {
            logger.error("error during resending email verification request error: {}", e.getMessage());
            return RESEND_VERIFICATION_EMAIL;
        }

        redirectAttributes.addFlashAttribute("errorMessage", "reset verification failed, please try re-register");
        return "redirect:/registration";
    }

    private void sendVerificationEmail(HttpServletRequest request, UserDTO userDTO) {
        String baseUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath(null)
                .build()
                .toUriString();
        String params =
                "email=" + encode(userDTO.email(), UTF_8) + "&token=" + encode(userDTO.verificationToken(), UTF_8);
        String verificationUrl = baseUrl + "/verify-email?" + params;
        String to = userDTO.email();
        String subject = "TechBuzz - Email verification";
        Map<String, Object> paramsMap = Map.of("", userDTO.name(), "verificationUrl", verificationUrl);
        emailService.sendEmail("email/verify-email", paramsMap, to, subject);
    }
}
