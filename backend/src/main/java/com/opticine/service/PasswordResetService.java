package com.opticine.service;

import com.opticine.entity.PasswordResetToken;
import com.opticine.entity.User;
import com.opticine.repository.PasswordResetTokenRepository;
import com.opticine.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.mail-from}")
    private String mailFrom;

    /** Token hết hạn sau 15 phút */
    private static final int TOKEN_EXPIRY_MINUTES = 15;

    @Transactional
    public void processForgotPassword(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Không tiết lộ email có tồn tại không — vẫn trả về thành công ở controller
            log.warn("Forgot password requested for non-existent email: {}", email);
            return;
        }

        User user = userOpt.get();

        // Xóa token cũ của user này (nếu có)
        tokenRepository.deleteAllByUser(user);

        // Tạo token mới
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES))
                .used(false)
                .build();
        tokenRepository.save(resetToken);

        // Gửi email
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        sendResetEmail(user, resetLink);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token không hợp lệ hoặc đã hết hạn."));

        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            throw new IllegalArgumentException("Liên kết đặt lại mật khẩu đã hết hạn. Vui lòng yêu cầu lại.");
        }

        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("Liên kết này đã được sử dụng. Vui lòng yêu cầu lại.");
        }

        // Cập nhật mật khẩu
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Đánh dấu token đã dùng
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("Password reset successfully for user: {}", user.getEmail());
    }

    private void sendResetEmail(User user, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(user.getEmail());
            helper.setSubject("Đặt lại mật khẩu - Opticine");
            helper.setText(buildResetEmailHtml(user, resetLink), true);
            mailSender.send(message);
            log.info("Password reset email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", user.getEmail(), e);
            // Log link để dev có thể test ngay cả khi email chưa cấu hình
            log.info("Password reset link (for dev): {}", resetLink);
        }
    }

    private String buildResetEmailHtml(User user, String resetLink) {
        String name = user.getFullName() != null ? user.getFullName() : user.getEmail();
        return """
                <div style="font-family:Arial,sans-serif;background:#0f172a;color:#e2e8f0;padding:24px">
                  <div style="max-width:600px;margin:0 auto;background:#111827;border-radius:12px;overflow:hidden;border:1px solid #334155">
                    <div style="background:#e11d48;color:white;padding:20px;text-align:center">
                      <h1 style="margin:0;font-size:24px">Opticine</h1>
                      <p style="margin:8px 0 0;opacity:0.9">Đặt lại mật khẩu</p>
                    </div>
                    <div style="padding:32px">
                      <p style="margin-bottom:8px">Xin chào <strong>%s</strong>,</p>
                      <p style="color:#94a3b8;line-height:1.6">
                        Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản Opticine của bạn.
                        Nhấn vào nút bên dưới để tạo mật khẩu mới.
                      </p>
                      <div style="text-align:center;margin:32px 0">
                        <a href="%s"
                           style="display:inline-block;background:#e11d48;color:white;text-decoration:none;padding:14px 32px;border-radius:8px;font-weight:bold;font-size:16px">
                          ĐẶT LẠI MẬT KHẨU
                        </a>
                      </div>
                      <p style="color:#64748b;font-size:13px;line-height:1.6">
                        Liên kết này sẽ hết hạn sau <strong>15 phút</strong>.<br/>
                        Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này. Tài khoản của bạn vẫn an toàn.
                      </p>
                      <hr style="border:none;border-top:1px solid #1e293b;margin:24px 0"/>
                      <p style="color:#475569;font-size:12px;text-align:center">
                        Nếu nút bên trên không hoạt động, hãy sao chép và dán liên kết sau vào trình duyệt:<br/>
                        <a href="%s" style="color:#e11d48;word-break:break-all">%s</a>
                      </p>
                    </div>
                  </div>
                </div>
                """.formatted(
                escape(name),
                resetLink,
                resetLink,
                resetLink
        );
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
