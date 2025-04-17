package com.backend.backend.mapper;

import com.backend.backend.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);

    // 使用 useGeneratedKeys 和 keyProperty 来获取自增主键
    @Insert("INSERT INTO user(username, password, nickname, avatar, phone, email, role, create_time, update_time) " +
            "VALUES(#{username}, #{password}, #{nickname}, #{avatar}, #{phone}, #{email}, #{role}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

} 