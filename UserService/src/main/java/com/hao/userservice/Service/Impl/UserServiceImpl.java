package com.hao.userservice.Service.Impl;

import com.hao.commonmodel.Model.User.*;
import com.hao.commonmodel.Model.User.constants.CredentialType;
import com.hao.commonmodel.Model.User.constants.UserType;
import com.hao.commonmodel.common.Page;
import com.hao.commonunits.utils.PageUtil;
import com.hao.commonunits.utils.PhoneUtil;
import com.hao.userservice.Dao.AppUserDao;
import com.hao.userservice.Dao.UserCredentialsDao;
import com.hao.userservice.Dao.UserRoleDao;
import com.hao.userservice.Service.SysPermissionService;
import com.hao.userservice.Service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.util.BeanUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author MuggleLee
 * @date 2019/7/21
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private AppUserDao appUserDao;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private SysPermissionService sysPermissionService;
    @Autowired
    private UserRoleDao userRoleDao;
    @Autowired
    private UserCredentialsDao userCredentialsDao;

    @Transactional
    @Override
    public void addAppUser(AppUser appUser) {
        String username = appUser.getUsername();
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        if (PhoneUtil.checkPhone(appUser.getPhone())) {
            throw new IllegalArgumentException("手机号码不正确");
        }

        if (username.contains("@")) {
            throw new IllegalArgumentException("用户名不能包含@");
        }

        if (username.contains("|")) {
            throw new IllegalArgumentException("用户名不能包含|字符");
        }

        if (StringUtils.isBlank(appUser.getPassword())) {
            throw new IllegalArgumentException("密码不能为空");
        }

        if (StringUtils.isBlank(appUser.getNickname())) {
            appUser.setNickname(username);
        }

        if (StringUtils.isBlank(appUser.getType())) {
            appUser.setType(UserType.APP.name());
        }

        UserCredential userCredential = userCredentialsDao.findByUsername(appUser.getUsername());
        if (userCredential != null) {
            throw new IllegalArgumentException("用户名已存在");
        }

        appUser.setPassword(passwordEncoder.encode(appUser.getPassword())); // 加密密码
        appUser.setEnabled(Boolean.TRUE);
        appUser.setCreateTime(new Date());
        appUser.setUpdateTime(appUser.getCreateTime());

        appUserDao.save(appUser);
        userCredentialsDao
                .save(new UserCredential(appUser.getUsername(), CredentialType.USERNAME.name(), appUser.getId()));
        log.info("添加用户：{}", appUser);
    }

    @Transactional
    @Override
    public void updateAppUser(AppUser appUser) {
        appUser.setPassword(passwordEncoder.encode(appUser.getPassword()));
        appUser.setUpdateTime(new Date());
        appUserDao.update(appUser);
        log.info("修改用户：{}",appUser);
    }

    @Transactional
    @Override
    public LoginAppUser findByUserName(String userName) {

        AppUser appUser = userCredentialsDao.findUserByUsername(userName);

        if(appUser != null){
            LoginAppUser loginAppUser = new LoginAppUser();
            BeanUtils.copyProperties(appUser,loginAppUser);
            Set<SysRole> sysRoles = userRoleDao.findRolesByUserId(appUser.getId());
            loginAppUser.setSysRoles(sysRoles);//设置角色

            if(!CollectionUtils.isEmpty(sysRoles)){
                Set<Long> roleIds = sysRoles.parallelStream().map(SysRole::getId).collect(Collectors.toSet());
                Set<SysPermission> sysPermissions = sysPermissionService.findByRoleIds(roleIds);
                if(!CollectionUtils.isEmpty(sysPermissions)){
                    Set<String> permissions = sysPermissions.parallelStream().map(SysPermission::getPermission).collect(Collectors.toSet());
                    loginAppUser.setPermissions(permissions);
                }
            }
            log.info("LoginAppUser：{}",loginAppUser);
            return loginAppUser;
        }
        return null;
    }

    @Override
    public AppUser findById(Long id) {
        return appUserDao.findById(id);
    }

    @Transactional
    @Override
    public void setRoleToUser(Long id, Set<Long> roleIds) {
        AppUser appUser = appUserDao.findById(id);
        if (appUser == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        userRoleDao.deleteUserRole(id, null);
        if (!CollectionUtils.isEmpty(roleIds)) {
            roleIds.forEach(roleId -> {
                userRoleDao.saveUserRoles(id, roleId);
            });
        }

        log.info("修改用户：{}的角色，{}", appUser.getUsername(), roleIds);
    }

    @Transactional
    @Override
    public void updatePassword(Long id, String oldPassword, String newPassword) {
        AppUser appUser = appUserDao.findById(id);
        if(StringUtils.isNoneBlank(oldPassword)){
            if(!passwordEncoder.matches(oldPassword,appUser.getPassword())){
                throw new IllegalArgumentException("旧密码错误");
            }
        }

        AppUser user = new AppUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setId(id);
        user.setUpdateTime(new Date());
        appUserDao.update(user);
        log.info("修改密码：{}", user);
    }

    @Override
    public Page<AppUser> findUsers(Map<String, Object> params) {
        int total = appUserDao.count(params);
        List<AppUser> list = Collections.emptyList();
        if (total > 0) {
            PageUtil.pageParamConver(params, true);

            list = appUserDao.findData(params);
        }
        return new Page<>(total, list);
    }

    @Override
    public Set<SysRole> findRolesByUserId(Long id) {
        return userRoleDao.findRolesByUserId(id);
    }

    @Transactional
    @Override
    public void bindingPhone(Long userId, String phone) {
        UserCredential userCredential = userCredentialsDao.findByUsername(phone);
        if (userCredential != null) {
            throw new IllegalArgumentException("手机号已被绑定");
        }

        AppUser appUser = appUserDao.findById(userId);
        appUser.setPhone(phone);

        updateAppUser(appUser);
        log.info("绑定手机号成功,username:{}，phone:{}", appUser.getUsername(), phone);

        // 绑定成功后，将手机号存到用户凭证表，后续可通过手机号+密码或者手机号+短信验证码登陆
        userCredentialsDao.save(new UserCredential(phone, CredentialType.PHONE.name(), userId));
    }
}
