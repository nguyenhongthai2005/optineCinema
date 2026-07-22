package com.opticine.service;

import com.opticine.dto.admin.staff.StaffCredentialResponse;
import com.opticine.dto.admin.staff.StaffRequest;
import com.opticine.dto.admin.staff.StaffResponse;
import com.opticine.entity.ContractType;
import com.opticine.entity.Role;
import com.opticine.entity.StaffAvailability;
import com.opticine.entity.StaffAssignment;
import com.opticine.entity.StaffPosition;
import com.opticine.entity.User;
import com.opticine.repository.RoleRepository;
import com.opticine.repository.StaffAvailabilityRepository;
import com.opticine.repository.StaffAssignmentRepository;
import com.opticine.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AdminStaffService {
    private static final String STAFF_ROLE = "ROLE_STAFF";
    private static final String STAFF_USERNAME_PREFIX = "STAFF";
    private static final Pattern STAFF_USERNAME_PATTERN = Pattern.compile("^STAFF(\\d+)$");
    private static final String PASSWORD_UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String PASSWORD_LOWER = "abcdefghijkmnpqrstuvwxyz";
    private static final String PASSWORD_DIGITS = "23456789";
    private static final String PASSWORD_SPECIAL = "@#$%&*!";
    private static final String PASSWORD_ALL = PASSWORD_UPPER + PASSWORD_LOWER + PASSWORD_DIGITS + PASSWORD_SPECIAL;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final StaffAvailabilityRepository staffAvailabilityRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;

    public AdminStaffService(UserRepository userRepository,
                             RoleRepository roleRepository,
                             PasswordEncoder passwordEncoder,
                             StaffAvailabilityRepository staffAvailabilityRepository,
                             StaffAssignmentRepository staffAssignmentRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.staffAvailabilityRepository = staffAvailabilityRepository;
        this.staffAssignmentRepository = staffAssignmentRepository;
    }

    public List<StaffResponse> search(String keyword, String status, String position, String contractType) {
        String normalized = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String normalizedStatus = StringUtils.hasText(status) ? normalizeStatus(status) : null;
        String normalizedPosition = StringUtils.hasText(position) ? normalizePosition(position).name() : null;
        String normalizedContractType = StringUtils.hasText(contractType) ? normalizeContractType(contractType).name() : null;
        return userRepository.searchStaff(normalized, normalizedStatus, normalizedPosition, normalizedContractType)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public StaffResponse getById(Long id) {
        return toResponse(findStaff(id));
    }

    @Transactional
    public synchronized StaffCredentialResponse create(StaffRequest request) {
        if (StringUtils.hasText(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (StringUtils.hasText(request.getPhone()) && userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("Phone already exists");
        }

        String username = generateNextUsername();
        String temporaryPassword = generateTemporaryPassword();
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(clean(request.getEmail()));
        user.setPhone(request.getPhone());
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setRoles(resolveStaffRole());
        user.setStatus(normalizeStatus(request.getStatus()));
        user.setEnabled("ACTIVE".equalsIgnoreCase(user.getStatus()));
        user.setGender(clean(request.getGender()));
        user.setDateOfBirth(request.getDateOfBirth());
        user.setAddress(clean(request.getAddress()));
        user.setStaffPosition(normalizePosition(coalesce(request.getPosition(), request.getStaffPosition())).name());
        user.setEmploymentType(normalizeContractType(coalesce(request.getContractType(), request.getEmploymentType())).name());

        try {
            User saved = userRepository.save(user);
            return toCredentialResponse(saved, temporaryPassword,
                    "Tạo tài khoản nhân viên thành công. Vui lòng lưu lại mật khẩu tạm thời vì hệ thống sẽ không hiển thị lại.");
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Username collision while creating staff. Please try again.");
        }
    }

    @Transactional
    public StaffResponse update(Long id, StaffRequest request) {
        User user = findStaff(id);
        if (StringUtils.hasText(request.getEmail()) && (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(request.getEmail()))) {
            userRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
                throw new IllegalArgumentException("Email already exists");
            });
        }
        if (StringUtils.hasText(request.getPhone()) && !request.getPhone().equals(user.getPhone())) {
            userRepository.findByPhone(request.getPhone()).ifPresent(existing -> {
                throw new IllegalArgumentException("Phone already exists");
            });
        }

        user.setFullName(request.getFullName());
        user.setEmail(clean(request.getEmail()));
        user.setPhone(request.getPhone());
        if (StringUtils.hasText(request.getStatus())) {
            applyStatus(user, request.getStatus());
        }
        user.setGender(clean(request.getGender()));
        user.setDateOfBirth(request.getDateOfBirth());
        user.setAddress(clean(request.getAddress()));
        user.setStaffPosition(normalizePosition(coalesce(request.getPosition(), request.getStaffPosition())).name());
        user.setEmploymentType(normalizeContractType(coalesce(request.getContractType(), request.getEmploymentType())).name());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public StaffResponse updateStatus(Long id, String status) {
        User user = findStaff(id);
        applyStatus(user, status);
        return toResponse(userRepository.save(user));
    }

    public String getNextUsername() {
        return generateNextUsername();
    }

    @Transactional
    public StaffCredentialResponse resetPassword(Long id) {
        User user = findStaff(id);
        String temporaryPassword = generateTemporaryPassword();
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        User saved = userRepository.save(user);
        return toCredentialResponse(saved, temporaryPassword,
                "Đặt lại mật khẩu nhân viên thành công. Vui lòng lưu lại mật khẩu tạm thời vì hệ thống sẽ không hiển thị lại.");
    }

    @Transactional
    public StaffCredentialResponse revoke(Long id) {
        User user = findStaff(id);
        String temporaryPassword = generateTemporaryPassword();
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setStatus("LOCKED");
        user.setEnabled(false);
        user.setFullName("Tài khoản đã thu hồi");
        user.setEmail(null);
        user.setPhone(null);
        user.setGender(null);
        user.setDateOfBirth(null);
        user.setAddress(null);
        user.setAvatarUrl(null);
        user.setStaffPosition(null);
        user.setEmploymentType(null);

        List<StaffAvailability> availability = staffAvailabilityRepository.findByStaffIdOrderByDayOfWeekAscStartTimeAsc(user.getId());
        availability.forEach(row -> {
            row.setStatus("INACTIVE");
            row.setNote(appendNote(row.getNote(), "Đã vô hiệu do tài khoản nhân viên bị thu hồi."));
        });
        staffAvailabilityRepository.saveAll(availability);

        List<StaffAssignment> futureAssignments = staffAssignmentRepository
                .findByStaffIdAndWorkDateGreaterThanEqualAndStatusIgnoreCase(user.getId(), LocalDate.now(), "SCHEDULED");
        futureAssignments.forEach(assignment -> {
            assignment.setStatus("CANCELLED");
            assignment.setNote(appendNote(assignment.getNote(), "Đã hủy do tài khoản nhân viên bị thu hồi."));
        });
        staffAssignmentRepository.saveAll(futureAssignments);

        User saved = userRepository.save(user);
        return toCredentialResponse(saved, temporaryPassword,
                "Đã thu hồi tài khoản nhân viên và hủy các phân công sắp tới.");
    }

    public List<Map<String, Object>> getAvailability(Long staffId) {
        User staff = findStaff(staffId);
        return staffAvailabilityRepository.findByStaffIdOrderByDayOfWeekAscStartTimeAsc(staff.getId())
                .stream()
                .map(this::toAvailabilityMap)
                .toList();
    }

    public List<Map<String, Object>> searchAvailability(String position, String contractType, String dayOfWeek) {
        String normalizedPosition = StringUtils.hasText(position) ? normalizePosition(position).name() : null;
        String normalizedContractType = StringUtils.hasText(contractType) ? normalizeContractType(contractType).name() : null;
        DayOfWeek day = StringUtils.hasText(dayOfWeek) ? DayOfWeek.valueOf(dayOfWeek.trim().toUpperCase(Locale.ROOT)) : null;
        List<StaffAvailability> rows = day == null
                ? staffAvailabilityRepository.findAll()
                : staffAvailabilityRepository.findByDayOfWeekAndStatusIgnoreCase(day, "ACTIVE");
        return rows.stream()
                .filter(row -> row.getStaff().getRoles() != null && row.getStaff().getRoles().stream().anyMatch(role -> STAFF_ROLE.equals(role.getName())))
                .filter(row -> normalizedPosition == null || normalizedPosition.equals(row.getStaff().getStaffPosition()))
                .filter(row -> normalizedContractType == null || normalizedContractType.equals(row.getStaff().getEmploymentType()))
                .map(this::toAvailabilityMap)
                .toList();
    }

    private User findStaff(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Staff not found"));
        boolean isStaff = user.getRoles() != null && user.getRoles().stream().anyMatch(role -> STAFF_ROLE.equals(role.getName()));
        if (!isStaff) {
            throw new IllegalArgumentException("User is not staff");
        }
        return user;
    }

    private Set<Role> resolveStaffRole() {
        Role role = roleRepository.findByName(STAFF_ROLE)
                .orElseGet(() -> roleRepository.save(Role.builder().name(STAFF_ROLE).build()));
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        return roles;
    }

    private String normalizeStatus(String status) {
        String normalized = StringUtils.hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : "ACTIVE";
        if ("BLOCKED".equals(normalized)) {
            return "LOCKED";
        }
        if (!Set.of("ACTIVE", "INACTIVE", "LOCKED").contains(normalized)) {
            throw new IllegalArgumentException("Invalid staff status");
        }
        return normalized;
    }

    private void applyStatus(User user, String status) {
        String normalized = normalizeStatus(status);
        user.setStatus(normalized);
        user.setEnabled("ACTIVE".equals(normalized));
    }

    private StaffPosition normalizePosition(String value) {
        try {
            return StaffPosition.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid staff position");
        }
    }

    private ContractType normalizeContractType(String value) {
        try {
            return ContractType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid contract type");
        }
    }

    private StaffResponse toResponse(User user) {
        String role = user.getRoles() == null ? null : user.getRoles().stream()
                .map(Role::getName)
                .findFirst()
                .orElse(null);
        return StaffResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .address(user.getAddress())
                .position(user.getStaffPosition())
                .positionLabel(positionLabel(user.getStaffPosition()))
                .contractType(user.getEmploymentType())
                .contractTypeLabel(contractTypeLabel(user.getEmploymentType()))
                .role(role)
                .status(user.getStatus())
                .enabled(user.getEnabled())
                .staffPosition(user.getStaffPosition())
                .employmentType(user.getEmploymentType())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private StaffCredentialResponse toCredentialResponse(User user, String temporaryPassword, String message) {
        return StaffCredentialResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .temporaryPassword(temporaryPassword)
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .position(user.getStaffPosition())
                .positionLabel(positionLabel(user.getStaffPosition()))
                .contractType(user.getEmploymentType())
                .contractTypeLabel(contractTypeLabel(user.getEmploymentType()))
                .message(message)
                .build();
    }

    private Map<String, Object> toAvailabilityMap(StaffAvailability availability) {
        User staff = availability.getStaff();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", availability.getId());
        map.put("staffId", staff.getId());
        map.put("staffName", staff.getFullName());
        map.put("username", staff.getUsername());
        map.put("position", staff.getStaffPosition());
        map.put("positionLabel", positionLabel(staff.getStaffPosition()));
        map.put("contractType", staff.getEmploymentType());
        map.put("contractTypeLabel", contractTypeLabel(staff.getEmploymentType()));
        map.put("dayOfWeek", availability.getDayOfWeek());
        map.put("startTime", availability.getStartTime());
        map.put("endTime", availability.getEndTime());
        map.put("note", availability.getNote());
        map.put("status", availability.getStatus());
        return map;
    }

    private String positionLabel(String value) {
        try {
            return StaffPosition.valueOf(value).getLabel();
        } catch (Exception ex) {
            return value;
        }
    }

    private String contractTypeLabel(String value) {
        try {
            return ContractType.valueOf(value).getLabel();
        } catch (Exception ex) {
            return value;
        }
    }

    private String generateNextUsername() {
        int max = userRepository.findGeneratedStaffUsernames().stream()
                .map(STAFF_USERNAME_PATTERN::matcher)
                .filter(Matcher::matches)
                .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
                .max()
                .orElse(0);

        int next = max + 1;
        String username;
        do {
            username = STAFF_USERNAME_PREFIX + String.format("%03d", next++);
        } while (userRepository.existsByUsername(username));
        return username;
    }

    private String generateTemporaryPassword() {
        char[] password = new char[12];
        password[0] = randomChar(PASSWORD_UPPER);
        password[1] = randomChar(PASSWORD_LOWER);
        password[2] = randomChar(PASSWORD_DIGITS);
        password[3] = randomChar(PASSWORD_SPECIAL);
        for (int i = 4; i < password.length; i++) {
            password[i] = randomChar(PASSWORD_ALL);
        }
        for (int i = password.length - 1; i > 0; i--) {
            int j = SECURE_RANDOM.nextInt(i + 1);
            char temp = password[i];
            password[i] = password[j];
            password[j] = temp;
        }
        return new String(password);
    }

    private char randomChar(String source) {
        return source.charAt(SECURE_RANDOM.nextInt(source.length()));
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String coalesce(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private String appendNote(String current, String addition) {
        return StringUtils.hasText(current) ? current + " " + addition : addition;
    }
}
