package RIKU.server.Security;

import RIKU.server.Entity.User.User;
import RIKU.server.Repository.UserRepository;
import RIKU.server.Util.BaseResponseStatus;
import RIKU.server.Util.Exception.Domain.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String studentId) throws UsernameNotFoundException {
        log.info("UserDetailsServiceImpl 진입");
        User user = userRepository.findByStudentId(studentId).orElseThrow(() ->
                new UserException(BaseResponseStatus.USER_NOT_FOUND));

        List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(user.getUserRole().getRole());

        return AuthMember.of(user.getId(), user.getStudentId(), user.getPassword(), authorities, user.getStatus());
    }
}
